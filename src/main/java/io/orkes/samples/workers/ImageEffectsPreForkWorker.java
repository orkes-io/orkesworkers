package io.orkes.samples.workers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import lombok.Data;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ImageEffectsPreForkWorker {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Data
    public static class ImageEffectsInput {
        private List<String> fileLocations;
        private List<Map<String, Object>> recipeInfos;
    }

    @WorkerTask("image_effects_prefork")
    @Tool(description = "Prepares dynamic tasks for applying effects to images")
    public Map<String, Object> imageEffectsPreFork(
            @ToolParam(description = "Input parameters for image effects") ImageEffectsInput input) {

        Map<String, Object> result = new HashMap<>();

        try {
            List<WorkflowTask> dynamicTasks = Lists.newArrayList();
            Map<String, Object> dynamicTasksInput = Maps.newHashMap();

            int i = 0;
            String dynamicTaskName = "image_effect";
            for (String fileLocation : input.getFileLocations()) {
                for (Map<String, Object> recipeInfo : input.getRecipeInfos()) {
                    String recipe = (String) recipeInfo.get("recipe");
                    Map<String, Object> recipeParameters = (Map<String, Object>) recipeInfo.get("recipeParameters");

                    String fileName = Paths.get(new URI(fileLocation).getPath()).getFileName().toString();
                    String taskRefName = String.format("%s_%s_%s_%d", dynamicTaskName, fileName, recipe, i++);
                    WorkflowTask dynamicTask = new WorkflowTask();
                    dynamicTask.setName(dynamicTaskName);
                    dynamicTask.setTaskReferenceName(taskRefName);
                    dynamicTasks.add(dynamicTask);

                    Map<String, Object> dynamicTaskInput = Maps.newHashMap();
                    dynamicTaskInput.put("fileLocation", fileLocation);
                    dynamicTaskInput.put("recipe", recipe);
                    dynamicTaskInput.put("recipeParameters", recipeParameters);

                    dynamicTasksInput.put(taskRefName, dynamicTaskInput);
                }
            }

            String currentTimeOnServer = Instant.now().toString();
            // We can't log directly, but we can include it in the output
            result.put("log", "This is a test log at time: " + currentTimeOnServer);
            result.put("dynamicTasks", dynamicTasks);
            result.put("dynamicTasksInput", dynamicTasksInput);

            return result;

        } catch (Exception e) {
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw, true);
            e.printStackTrace(pw);

            // Add error info to result
            result.put("error", e.getMessage());
            result.put("stackTrace", sw.getBuffer().toString());
            throw new RuntimeException("Failed to prepare image effects tasks: " + e.getMessage(), e);
        }
    }
}