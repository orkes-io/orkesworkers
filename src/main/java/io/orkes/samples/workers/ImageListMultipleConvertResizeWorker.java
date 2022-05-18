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
import org.im4java.core.ConvertCmd;
import org.im4java.core.IMOperation;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class ImageListMultipleConvertResizeWorker implements Worker {

    private ObjectMapper objectMapper = new ObjectMapper();


    @Override
    public String getTaskDefName() {
        return "image_list_multiple_convert_resize";
    }

    @Override
    public TaskResult execute(Task task) {

        TaskResult result = new TaskResult(task);

        try {
            List<String> fileLocations = (List<String>) task.getInputData().get("fileLocations");
            List<String> outputFormats = (List<String>)(task.getInputData().get("outputFormats"));
            List<Size> outputSizes = (List<Size>) objectMapper.convertValue(task.getInputData().get("outputSizes"), new TypeReference<List<Size>>(){});
            Boolean maintainAspectRatio = Boolean.valueOf((String)task.getInputData().get("maintainAspectRatio"));

            List<WorkflowTask> dynamicTasks =  Lists.newArrayList();
            Map<String, Object> dynamicTasksInput = Maps.newHashMap();

            int i=0;
            String dynamicTaskName = "image_multiple_convert_resize";
            for (String fileLocation :
                    fileLocations) {

                    String fileName = Paths.get(new URI(fileLocation).getPath()).getFileName().toString();
                    String taskRefName = String.format("%s_%s_%d",dynamicTaskName, fileName, i++);
                    WorkflowTask dynamicTask = new WorkflowTask();
                    dynamicTask.setName(dynamicTaskName);
                    dynamicTask.setTaskReferenceName(taskRefName);
                    dynamicTask.setWorkflowTaskType(TaskType.SUB_WORKFLOW);
                    SubWorkflowParams subWorkflowParams = new SubWorkflowParams();
                    subWorkflowParams.setName("image_multiple_convert_resize");
                    dynamicTask.setSubWorkflowParam(subWorkflowParams);
                    dynamicTasks.add(dynamicTask);

                    Map<String, Object> dynamicTaskInput = Maps.newHashMap();
                    dynamicTaskInput.put("fileLocation", fileLocation);
                    dynamicTaskInput.put("outputFormats", outputFormats);
                    dynamicTaskInput.put("outputSizes", outputSizes);
                    dynamicTaskInput.put("maintainAspectRatio", maintainAspectRatio);

                    dynamicTasksInput.put(taskRefName,dynamicTaskInput );
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