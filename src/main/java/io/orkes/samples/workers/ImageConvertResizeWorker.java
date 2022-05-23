package io.orkes.samples.workers;

import com.amazonaws.regions.Regions;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import io.orkes.samples.utils.S3Utils;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IMOperation;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.UUID;

@Component
public class ImageConvertResizeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "image_convert_resize";
    }

    @Override
    public TaskResult execute(Task task) {

        TaskResult result = new TaskResult(task);

        try {
            String fileLocation = (String) task.getInputData().get("fileLocation");
            Integer width = (Integer) task.getInputData().get("outputWidth");
            Integer height = (Integer) task.getInputData().get("outputHeight");

            String outputFormat = (String) task.getInputData().get("outputFormat");

            String outputFileName = "/tmp/" + UUID.randomUUID().toString() + "."+outputFormat;


            String s3BucketName = "image-processing-orkes";

            resizeImage(fileLocation, width, height, outputFileName);

            String url = S3Utils.uploadToS3(outputFileName, Regions.US_EAST_1, s3BucketName);
            result.setStatus(TaskResult.Status.COMPLETED);
            result.addOutputData("fileLocation", url);
            result.addOutputData("outputWidth", width);
            result.addOutputData("outputHeight", height);
            result.addOutputData("outputFormat", outputFormat);

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
