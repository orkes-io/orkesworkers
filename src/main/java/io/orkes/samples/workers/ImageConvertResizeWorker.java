package io.orkes.samples.workers;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.google.common.io.Files;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import io.orkes.samples.utils.S3Utils;
import lombok.SneakyThrows;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IMOperation;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Paths;
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

        try {
            String fileLocation = (String) task.getInputData().get("fileLocation");
            Integer width = (Integer) task.getInputData().get("outputWidth");
            Integer height = (Integer) task.getInputData().get("outputHeight");

            String outputFileName = "/tmp/" + UUID.randomUUID().toString() + ".png";
            String s3BucketName = "image-processing-sandbox";

            //resizeImage(fileLocation, width, height, outputFileName);
//            vibrant(fileLocation, 3, outputFileName);
            //sepia(fileLocation, 80.0, outputFileName);
            S3Utils.uploadtToS3(outputFileName, Regions.US_EAST_1, s3BucketName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        String currentTimeOnServer = Instant.now().toString();
        result.log("This is a test log at time: " + currentTimeOnServer);
        result.addOutputData("currentTimeOnServer", currentTimeOnServer);
        result.addOutputData("message", "Hello World!");
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
