package io.orkes.samples.workers;

import com.amazonaws.regions.Regions;
import com.google.common.primitives.Doubles;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;
import com.netflix.conductor.sdk.workflow.task.OutputParam;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import io.orkes.samples.utils.S3Utils;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IMOperation;
import org.springframework.stereotype.Component;
import lombok.Data;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class ImageConvertResizeWorker {

    @Data
    public static class ImageConversionInput {
        private String fileLocation;
        private Object outputWidth;
        private Object outputHeight;
        private String outputFormat;
    }

    @WorkerTask("image_convert_resize")
    @Tool(description = "Converts and resizes an image to specified dimensions and format")
    public Map<String, Object> imageConvertResize(
            @ToolParam(description = "Input parameters for image conversion") ImageConversionInput input) {

        Map<String, Object> result = new HashMap<>();

        try {
            Integer width = Doubles.tryParse(input.getOutputWidth().toString()).intValue();
            Integer height = Doubles.tryParse(input.getOutputHeight().toString()).intValue();
            String outputFormat = input.getOutputFormat();
            String outputFileName = "/tmp/" + UUID.randomUUID().toString() + "." + outputFormat;
            String s3BucketName = "image-processing-orkes";

            resizeImage(input.getFileLocation(), width, height, outputFileName);

            String url = S3Utils.uploadToS3(outputFileName, Regions.US_EAST_1, s3BucketName);

            result.put("fileLocation", url);
            result.put("outputWidth", width);
            result.put("outputHeight", height);
            result.put("outputFormat", outputFormat);

            return result;

        } catch (Exception e) {
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw, true);
            e.printStackTrace(pw);

            // Add error info to result
            result.put("error", e.getMessage());
            result.put("stackTrace", sw.getBuffer().toString());
            throw new RuntimeException("Failed to convert/resize image: " + e.getMessage(), e);
        }
    }

    private void resizeImage(String inputImage, Integer width, Integer height, String outputImage) throws Exception {
        ConvertCmd cmd = new ConvertCmd();

        IMOperation op = new IMOperation();
        op.addImage(inputImage);
        op.resize(width, height);
        op.addImage(outputImage);

        cmd.run(op);
    }

    private void vibrant(String inputImage, Integer vibrance, String outputImage) throws Exception {
        ConvertCmd cmd = new ConvertCmd();

        IMOperation op = new IMOperation();
        op.quiet();
        op.addImage(inputImage);
        op.colorspace("HCL");
        op.channel("g");
        if (vibrance < 0) {
            op.p_sigmoidalContrast(Double.valueOf(Math.abs(vibrance)), 0.0);
        } else {
            op.sigmoidalContrast(Double.valueOf(Math.abs(vibrance)), 0.0);
        }
        op.p_channel();
        op.colorspace("sRGB");
        op.p_repage();
        op.addImage(outputImage);

        cmd.run(op);
    }

    private void sepia(String inputImage, Double sepiaIntensityThreshold, String outputImage) throws Exception {
        ConvertCmd cmd = new ConvertCmd();

        IMOperation op = new IMOperation();
        op.quiet();
        op.addImage(inputImage);
        op.sepiaTone(sepiaIntensityThreshold);
        op.addImage(outputImage);

        cmd.run(op);
    }
}