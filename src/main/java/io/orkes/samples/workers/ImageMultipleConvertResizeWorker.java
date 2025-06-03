package io.orkes.samples.workers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import io.orkes.samples.utils.Size;
import org.springframework.stereotype.Component;
import lombok.Data;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ImageMultipleConvertResizeWorker {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Data
    public static class ImageMultipleConversionInput {
        private String fileLocation;
        private List<String> outputFormats;
        private List<Size> outputSizes;
        private Boolean maintainAspectRatio;
    }

    @WorkerTask("image_multiple_convert_resize")
    @Tool(description = "Creates dynamic tasks for converting a single image to multiple formats and sizes")
    public Map<String, Object> imageMultipleConvertResize(
            @ToolParam(description = "Input parameters for multiple image conversion options") ImageMultipleConversionInput input) {

        Map<String, Object> result = new HashMap<>();

        try {
            List<WorkflowTask> dynamicTasks = Lists.newArrayList();
            Map<String, Object> dynamicTasksInput = Maps.newHashMap();

            int i = 0;
            String dynamicTaskName = "image_convert_resize";
            for (String outputFormat : input.getOutputFormats()) {
                for (Size size : input.getOutputSizes()) {
                    String taskRefName = String.format("%s_%s_%sx%s_%d", dynamicTaskName, outputFormat, size.width, size.height, i++);
                    WorkflowTask dynamicTask = new WorkflowTask();
                    dynamicTask.setName(dynamicTaskName);
                    dynamicTask.setTaskReferenceName(taskRefName);
                    dynamicTasks.add(dynamicTask);

                    Map<String, Object> dynamicTaskInput = Maps.newHashMap();
                    dynamicTaskInput.put("fileLocation", input.getFileLocation());
                    dynamicTaskInput.put("outputFormat", outputFormat);
                    dynamicTaskInput.put("outputWidth", size.width);
                    dynamicTaskInput.put("outputHeight", size.height);
                    dynamicTaskInput.put("maintainAspectRatio", input.getMaintainAspectRatio());

                    dynamicTasksInput.put(taskRefName, dynamicTaskInput);
                }
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
            throw new RuntimeException("Failed to prepare multiple image conversion tasks: " + e.getMessage(), e);
        }
    }
}