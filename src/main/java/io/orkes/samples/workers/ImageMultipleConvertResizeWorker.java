package io.orkes.samples.workers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import io.orkes.samples.utils.Size;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IMOperation;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class ImageMultipleConvertResizeWorker implements Worker {

    private ObjectMapper objectMapper = new ObjectMapper();


    @Override
    public String getTaskDefName() {
        return "image_multiple_convert_resize";
    }

    @Override
    public TaskResult execute(Task task) {

        TaskResult result = new TaskResult(task);

        try {
            String fileLocation = (String) task.getInputData().get("fileLocation");
            List<String> outputFormats = (List<String>)(task.getInputData().get("outputFormats"));
            List<Size> outputSizes = (List<Size>) objectMapper.convertValue(task.getInputData().get("outputSizes"), new TypeReference<List<Size>>(){});
            Boolean maintainAspectRatio = Boolean.valueOf((String)task.getInputData().get("maintainAspectRatio"));

            List<WorkflowTask> dynamicTasks =  Lists.newArrayList();
            Map<String, Object> dynamicTasksInput = Maps.newHashMap();

            int i=0;
            String dynamicTaskName = "image_convert_resize";
            for (String outputFormat :
                    outputFormats) {
                for (Size size:
                     outputSizes) {
                    String taskRefName = String.format("%s_%s_%sx%s_%d",dynamicTaskName, outputFormat, size.width, size.height, i++);
                    WorkflowTask dynamicTask = new WorkflowTask();
                    dynamicTask.setName(dynamicTaskName);
                    dynamicTask.setTaskReferenceName(taskRefName);
                    dynamicTasks.add(dynamicTask);

                    Map<String, Object> dynamicTaskInput = Maps.newHashMap();
                    dynamicTaskInput.put("fileLocation", fileLocation);
                    dynamicTaskInput.put("outputFormat", outputFormat);
                    dynamicTaskInput.put("outputWidth", size.width);
                    dynamicTaskInput.put("outputHeight", size.height);
                    dynamicTaskInput.put("maintainAspectRatio", maintainAspectRatio);

                    dynamicTasksInput.put(taskRefName,dynamicTaskInput );
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


    private void resizeImage(String inputImage, Integer width, Integer height, String outputImage ) throws Exception {
        ConvertCmd cmd = new ConvertCmd();

        IMOperation op = new IMOperation();
        op.addImage(inputImage);
        op.resize(width,height);
        op.addImage(outputImage);

        cmd.run(op);
    }

    private void vibrant(String inputImage, Integer vibrance, String outputImage ) throws Exception {
        ConvertCmd cmd = new ConvertCmd();

        IMOperation op = new IMOperation();
        op.quiet();
        op.addImage(inputImage);
        op.colorspace("HCL");
        op.channel("g");
        if(vibrance < 0) {
            op.p_sigmoidalContrast(Double.valueOf(Math.abs(vibrance)), 0.0);
        }else {
            op.sigmoidalContrast(Double.valueOf(Math.abs(vibrance)), 0.0);
        }
        op.p_channel();
        op.colorspace("sRGB");
        op.p_repage();
        op.addImage(outputImage);

        cmd.run(op);
    }

    private void sepia(String inputImage, Double sepiaIntensityThreshold, String outputImage ) throws Exception {
        ConvertCmd cmd = new ConvertCmd();

        IMOperation op = new IMOperation();
        op.quiet();
        op.addImage(inputImage);
        op.sepiaTone(sepiaIntensityThreshold);
        op.addImage(outputImage);

        cmd.run(op);
    }
}
