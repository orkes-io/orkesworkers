package io.orkes.samples.workers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import com.netflix.conductor.common.metadata.tasks.TaskType;
import com.netflix.conductor.common.metadata.workflow.SubWorkflowParams;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import io.orkes.samples.utils.Size;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class ImageEffectsPreForkWorker implements Worker {

    private ObjectMapper objectMapper = new ObjectMapper();


    @Override
    public String getTaskDefName() {
        return "image_effects_prefork";
    }

    @Override
    public TaskResult execute(Task task) {

        TaskResult result = new TaskResult(task);

        try {
            List<String> fileLocations = (List<String>) task.getInputData().get("fileLocations");
            List<Map<String, Object>> recipeInfos = (List<Map<String, Object>>) task.getInputData().get("recipeInfos");

            List<WorkflowTask> dynamicTasks =  Lists.newArrayList();
            Map<String, Object> dynamicTasksInput = Maps.newHashMap();

            int i=0;
            String dynamicTaskName = "image_effect";
            for (String fileLocation :
                    fileLocations) {

                for (Map<String, Object> recipeInfo :
                        recipeInfos) {

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


            result.setStatus(TaskResult.Status.COMPLETED);
            String currentTimeOnServer = Instant.now().toString();
            result.log("This is a test log at time: " + currentTimeOnServer);
            result.addOutputData("dynamicTasks", dynamicTasks);
            result.addOutputData("dynamicTasksInput", dynamicTasksInput);

        } catch (Exception e) {
            e.printStackTrace();
            result.setStatus(TaskResult.Status.FAILED);
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw, true);
            e.printStackTrace(pw);
            result.log(sw.getBuffer().toString());
        }
        return result;
    }

}