package io.orkes.samples.workers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;
import lombok.Data;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class VideoRecipesPreForkWorker {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Data
    public static class VideoRecipesPreForkInput {
        private List<String> fileLocations;
        private List<Map<String, Object>> recipeInfos;
    }

    @WorkerTask("video_recipes_prefork")
    @Tool(description = "Creates dynamic tasks for processing videos with different recipes")
    public Map<String, Object> executeVideoRecipesPreFork(
            @ToolParam(description = "Input parameters for video recipe processing") VideoRecipesPreForkInput input) {

        Map<String, Object> result = new HashMap<>();

        try {
            List<String> fileLocations = input.getFileLocations();
            List<Map<String, Object>> recipeInfos = input.getRecipeInfos();

            List<WorkflowTask> dynamicTasks = Lists.newArrayList();
            Map<String, Object> dynamicTasksInput = Maps.newHashMap();

            int i = 0;
            String dynamicTaskName = "video_recipes";
            for (String fileLocation : fileLocations) {
                for (Map<String, Object> recipeInfo : recipeInfos) {
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

            // Add outputs to result
            result.put("dynamicTasks", dynamicTasks);
            result.put("dynamicTasksInput", dynamicTasksInput);
            result.put("log", "This is a test log at time: " + currentTimeOnServer);

        } catch (Exception e) {
            e.printStackTrace();
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw, true);
            e.printStackTrace(pw);

            // Add error details to result
            result.put("error", sw.getBuffer().toString());
            throw new RuntimeException("Failed to execute video recipes prefork: " + e.getMessage(), e);
        }

        return result;
    }
}