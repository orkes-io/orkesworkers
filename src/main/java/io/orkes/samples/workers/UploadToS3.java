package io.orkes.samples.workers;

import org.im4java.core.ConvertCmd;
import org.im4java.core.IMOperation;
import org.springframework.stereotype.Component;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import javax.imageio.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import com.amazonaws.regions.Regions;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse.File;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;
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
import java.util.HashMap;
import java.util.Map;


@Component
@Slf4j
public class UploadToS3 {
    private BufferedImage image=null;

    @Data
    public static class UploadToS3Input {
        private String fileLocation;
    }

    @WorkerTask("upload_toS3")
    @Tool(description = "Uploads an image file to S3 storage")
    public Map<String, Object> executeUploadToS3(
            @ToolParam(description = "Input parameters for S3 upload") UploadToS3Input input) {

        Map<String, Object> result = new HashMap<>();
        String fileLocation = input.getFileLocation();

        try {
            openImage(fileLocation);
            String s3BucketName = "image-processing-sandbox";
            String url = S3Utils.uploadToS3(fileLocation, Regions.US_EAST_1, s3BucketName);
            result.put("fileLocation", url);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("error", e.getMessage());

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw, true);
            e.printStackTrace(pw);
            result.put("stackTrace", sw.toString());

            throw new RuntimeException("Failed to upload to S3: " + e.getMessage(), e);
        }

        return result;
    }

    private void openImage(String inputImage) throws Exception {
        ConvertCmd cmd = new ConvertCmd();
        IMOperation op = new IMOperation();
        op.addImage(inputImage);
        cmd.run(op);
    }
}