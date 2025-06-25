package io.orkes.samples.workers;

import com.amazonaws.regions.Regions;
import com.google.common.io.Files;
import com.google.common.primitives.Doubles;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import io.orkes.samples.utils.S3Utils;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IMOperation;
import org.springframework.stereotype.Component;
import lombok.Data;

import java.io.*;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

enum RECIPE {
    SEPIA("sepia"),
    VIBRANT("vibrant"),
    WATERMARK("watermark");

    private final String recipeName;

    RECIPE(final String recipeName) {
        this.recipeName = recipeName;
    }

    @Override
    public String toString() {
        return recipeName;
    }

    public static RECIPE fromString(String recipeName) {
        for (RECIPE b : RECIPE.values()) {
            if (b.recipeName.equalsIgnoreCase(recipeName)) {
                return b;
            }
        }
        return null;
    }
}

@Component
public class ImageEffectWorker {

    public Set<RECIPE> supportedRecipes = EnumSet.allOf(RECIPE.class);

    @Data
    public static class ImageEffectInput {
        private String fileLocation;
        private String recipe;
        private Map<String, Object> recipeParameters;
    }

    @WorkerTask("image_effect")
    @Tool(description = "Applies various effects (sepia, vibrant, watermark) to an image")
    public Map<String, Object> applyImageEffect(
            @ToolParam(description = "Input parameters for applying image effects") ImageEffectInput input) {

        Map<String, Object> result = new HashMap<>();

        try {
            String fileLocation = input.getFileLocation();
            String recipeName = input.getRecipe().toLowerCase();
            Map<String, Object> recipeParameters = input.getRecipeParameters();

            RECIPE recipe = validateRecipeNames(recipeName);

            String fileExtension = Files.getFileExtension(fileLocation);
            String outputFileName = "/tmp/" + UUID.randomUUID().toString() + "-" + recipe.name() + "." + fileExtension;

            if (recipe == RECIPE.SEPIA) {
                Integer sepiaIntensityThreshold = Doubles.tryParse(recipeParameters.get("sepiaIntensityThreshold").toString()).intValue();
                sepia(fileLocation, sepiaIntensityThreshold, outputFileName);
            } else if (recipe == RECIPE.VIBRANT) {
                Integer vibrance = Doubles.tryParse(recipeParameters.get("vibrance").toString()).intValue();
                vibrant(fileLocation, vibrance, outputFileName);
            } else if (recipe == RECIPE.WATERMARK) {
                String watermarkFileLocation = ((String) recipeParameters.get("watermarkFileLocation"));
                String gravity = ((String) recipeParameters.get("gravity"));
                watermark(fileLocation, watermarkFileLocation, outputFileName, gravity);
            }

            String s3BucketName = "image-processing-orkes";

            String url = S3Utils.uploadToS3(outputFileName, Regions.US_EAST_1, s3BucketName);

            result.put("fileLocation", url);
            result.put("recipe", recipe);
            result.put("recipeParameters", recipeParameters);

            return result;

        } catch (Exception e) {
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw, true);
            e.printStackTrace(pw);

            // Add error info to result
            result.put("error", e.getMessage());
            result.put("stackTrace", sw.getBuffer().toString());
            throw new RuntimeException("Failed to apply image effect: " + e.getMessage(), e);
        }
    }

    private RECIPE validateRecipeNames(String recipeName) throws Exception {
        RECIPE recipe = RECIPE.fromString(recipeName);
        if (!supportedRecipes.contains(recipe)) {
            throw new Exception("Recipe: " + recipeName + " not supported. Supported recipes: " + supportedRecipes);
        }
        return recipe;
    }

    private void sepia(String inputImage, Integer sepiaIntensityThreshold, String outputImage) throws Exception {
        ConvertCmd cmd = new ConvertCmd();

        IMOperation op = new IMOperation();
        op.quiet();
        op.addImage(inputImage);
        op.addRawArgs("-sepia-tone", sepiaIntensityThreshold.toString() + "%");
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

    public void watermark(String inputFileLocation, String watermarkFileLocation, String outputFileLocation, String gravity) throws Exception {
        String cmd = "convert " +
                inputFileLocation +
                " " +
                watermarkFileLocation +
                " +distort affine \"0,0 0,0 %[w],%[h] %[fx:t?v.w*(u.h/v.h*0.1):s.w],%[fx:t?v.h*(u.h/v.h*0.1):s.h]\"" +
                "  -shave 1 -gravity " +
                gravity +
                " -geometry +10+10 -composite " +
                outputFileLocation;

        ProcessBuilder builder = new ProcessBuilder();
        builder.command("sh", "-c", cmd);

        Process process = builder.start();
        String output = loadStream(process.getInputStream());
        String error = loadStream(process.getErrorStream());

        int rc = process.waitFor();
        if (rc != 0) {
            throw new Exception(error);
        }
    }

    private static String loadStream(InputStream s) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(s));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null)
            sb.append(line).append("\n");
        return sb.toString();
    }
}