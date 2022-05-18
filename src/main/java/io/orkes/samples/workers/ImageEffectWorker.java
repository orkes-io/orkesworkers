package io.orkes.samples.workers;

import com.amazonaws.regions.Regions;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import io.orkes.samples.utils.S3Utils;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IMOperation;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


enum RECIPE {
    SEPIA("sepia"),
    VIBRANT("vibrant")
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
                Double sepiaIntensityThreshold = ((Double) recipeParameters.get("sepiaIntensityThreshold"));
                sepia(fileLocation, sepiaIntensityThreshold, outputFileName);
            } else if(recipe == RECIPE.VIBRANT) {
                Integer vibrance = ((Integer) recipeParameters.get("vibrance"));
                vibrant(fileLocation, vibrance, outputFileName);
            }

            String s3BucketName = "image-processing-orkes";

            String url = S3Utils.uploadToS3(outputFileName, Regions.US_EAST_1, s3BucketName);
            result.setStatus(TaskResult.Status.COMPLETED);
            result.addOutputData("fileLocation", url);

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

    private void sepia(String inputImage, Double sepiaIntensityThreshold, String outputImage ) throws Exception {
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
}