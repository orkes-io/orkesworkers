package io.orkes.samples.workers;

import org.im4java.core.ConvertCmd;
import org.im4java.core.IMOperation;
import org.springframework.stereotype.Component;

import javax.imageio.*;
import lombok.extern.slf4j.Slf4j;
import com.amazonaws.regions.Regions;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse.File;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import io.orkes.samples.utils.S3Utils;
import java.io.*;
import java.awt.*;
import java.awt.image.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;


@Component
public class UploadToS3 implements Worker {
    private BufferedImage image=null;
    @Override
    public String getTaskDefName() {
        return "upload_toS3";
    }

    @Override
    public TaskResult execute(Task task) {
        
        TaskResult result = new TaskResult(task);
            String fileLocation = (String) task.getInputData().get("fileLocation");
            try {
                openImage(fileLocation);
                String s3BucketName = "image-processing-sandbox";
                String url = S3Utils.uploadToS3(fileLocation, Regions.US_EAST_1, s3BucketName);
                result.setStatus(TaskResult.Status.COMPLETED);
                result.addOutputData("fileLocation", url);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }


        return result;
    }
    private void openImage(String inputImage ) throws Exception {
        ConvertCmd cmd = new ConvertCmd();
        IMOperation op = new IMOperation();
        op.addImage(inputImage);
        cmd.run(op);
    }


    
}
