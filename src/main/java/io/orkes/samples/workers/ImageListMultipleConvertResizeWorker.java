package io.orkes.samples.workers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.netflix.conductor.common.metadata.tasks.TaskType;
import com.netflix.conductor.common.metadata.workflow.SubWorkflowParams;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import io.orkes.samples.utils.Size;
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
public class ImageListMultipleConvertResizeWorker {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Data
    public static class ImageListConversionInput {
        private List<String> fileLocations;
        private List<String> outputFormats;
        private List<Size> outputSizes;
        private Boolean maintainAspectRatio;
    }

    @WorkerTask("image_list_multiple_convert_resize")
    @Tool(description = "Creates dynamic tasks for converting and resizing multiple images to various formats and sizes")
    public Map<String, Object> imageListMultipleConvertResize(
            @ToolParam(description = "Input parameters for multiple image conversions") ImageListConversionInput input) {

        Map<String, Object> result = new HashMap<>();

        try {
            List<WorkflowTask> dynamicTasks = Lists.newArrayList();
            Map<String, Object> dynamicTasksInput = Maps.newHashMap();

            int i = 0;
            String dynamicTaskName = "image_multiple_convert_resize";
            for (String fileLocation : input.getFileLocations()) {
                String fileName = Paths.get(new URI(fileLocation).getPath()).getFileName().toString();
                String taskRefName = String.format("%s_%s_%d", dynamicTaskName, fileName, i++);
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
                dynamicTaskInput.put("outputFormats", input.getOutputFormats());
                dynamicTaskInput.put("outputSizes", input.getOutputSizes());
                dynamicTaskInput.put("maintainAspectRatio", input.getMaintainAspectRatio());

                dynamicTasksInput.put(taskRefName, dynamicTaskInput);
            }

            String currentTimeOnServer = Instant.now().toString();
            // Include log in the output data
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
            throw new RuntimeException("Failed to prepare image conversion tasks: " + e.getMessage(), e);
        }
    }
}