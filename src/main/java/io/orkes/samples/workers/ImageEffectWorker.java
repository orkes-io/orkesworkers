package io.orkes.samples.workers;

import com.amazonaws.regions.Regions;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import io.orkes.samples.utils.S3Utils;
import org.im4java.core.CompositeCmd;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IMOperation;
import org.im4java.core.ImageMagickCmd;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.Executors;


enum RECIPE {
    SEPIA("sepia"),
    VIBRANT("vibrant"),
    WATERMARK("watermark")
    ;

    private final String recipeName;


    RECIPE(final String recipeName) {
        this.recipeName = recipeName;
    }

    /* (non-Javadoc)
     * @see java.lang.Enum#toString()
     */
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
public class ImageEffectWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "image_effect";
    }


    public Set<RECIPE> supportedRecipes = EnumSet.allOf(RECIPE.class);

    @Override
    public TaskResult execute(Task task) {

        TaskResult result = new TaskResult(task);

        try {
            String fileLocation = (String) task.getInputData().get("fileLocation");
            String recipeName = ((String) task.getInputData().get("recipe")).toLowerCase();

            RECIPE recipe = validateRecipeNames(recipeName);
            Map<String, Object> recipeParameters = (Map<String, Object>) task.getInputData().get("recipeParameters");

            String fileExtension = Files.getFileExtension(fileLocation);
            String outputFileName = "/tmp/" + UUID.randomUUID().toString() + "-" + recipe.name() + "."+fileExtension;

            if(recipe == RECIPE.SEPIA) {
                Integer sepiaIntensityThreshold = ((Integer) recipeParameters.get("sepiaIntensityThreshold"));
                sepia(fileLocation, sepiaIntensityThreshold, outputFileName);
            } else if(recipe == RECIPE.VIBRANT) {
                Integer vibrance = ((Integer) recipeParameters.get("vibrance"));
                vibrant(fileLocation, vibrance, outputFileName);
            } else if(recipe == RECIPE.WATERMARK) {
                String watermarkFileLocation = ((String) recipeParameters.get("watermarkFileLocation"));
                String gravity = ((String) recipeParameters.get("gravity"));
                watermark(fileLocation, watermarkFileLocation, outputFileName, gravity);
            }

            String s3BucketName = "image-processing-orkes";

            String url = S3Utils.uploadToS3(outputFileName, Regions.US_EAST_1, s3BucketName);
            result.setStatus(TaskResult.Status.COMPLETED);
            result.addOutputData("fileLocation", url);
            result.addOutputData("recipe", recipe);
            result.addOutputData("recipeParameters", recipeParameters);

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

    private RECIPE validateRecipeNames(String recipeName) throws Exception {
        RECIPE recipe = RECIPE.fromString(recipeName);
        if(!supportedRecipes.contains(recipe)) {
            throw new Exception("Recipe: " + recipeName + " not supported. Supported recipes: " + supportedRecipes);
        }
        return recipe;
    }

    private void sepia(String inputImage, Integer sepiaIntensityThreshold, String outputImage ) throws Exception {
        ConvertCmd cmd = new ConvertCmd();

        IMOperation op = new IMOperation();
        op.quiet();
        op.addImage(inputImage);
//        op.sepiaTone(sepiaIntensityThreshold);
        op.addRawArgs("-sepia-tone", sepiaIntensityThreshold.toString()+"%");
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

    public void watermark(String inputFileLocation, String watermarkFileLocation, String  outputFileLocation, String gravity )  throws  Exception {

        String cmd = "magick " +
                        inputFileLocation +
                        " -set option:logowidth \"%[fx:int(w*0.25)]\" \\( " +
                        watermarkFileLocation +
                        " -resize \"%[logowidth]x\" \\) -gravity " +
                        gravity +
                        " -geometry +10+10 -composite " +
                        outputFileLocation;

        ProcessBuilder builder = new ProcessBuilder();
        builder.command("sh","-c",cmd);


        System.out.println(builder.command().toString());

        Process process = builder.start();
        assert (process.waitFor() >= 0);

    }
}
