package io.orkes.samples.workers;

import com.amazonaws.regions.Regions;
import com.google.common.base.Strings;
import com.google.common.io.Files;
import com.google.common.primitives.Doubles;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;
import io.orkes.samples.utils.S3Utils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.core.util.FileUtils;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IMOperation;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;


enum VIDEO_RECIPE {
    //    THUMBNAIL_GENERATE("thumbnail_generate"),
//    SCENE_DETECT("scene_detect"),
    WATERMARK("watermark"),
    TRANSCODE("transcode"),
    ;

    private final String recipeName;


    VIDEO_RECIPE(final String recipeName) {
        this.recipeName = recipeName;
    }

    /* (non-Javadoc)
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString() {
        return recipeName;
    }

    public static VIDEO_RECIPE fromString(String recipeName) {
        for (VIDEO_RECIPE b : VIDEO_RECIPE.values()) {
            if (b.recipeName.equalsIgnoreCase(recipeName)) {
                return b;
            }
        }
        return null;
    }
}

@Component
@Slf4j
public class VideoRecipeWorker {

    public Set<VIDEO_RECIPE> supportedRecipes = EnumSet.allOf(VIDEO_RECIPE.class);

    @Data
    public static class VideoRecipeInput {
        private String fileLocation;
        private String recipe;
        private Map<String, Object> recipeParameters;
    }

    @WorkerTask("video_recipes")
    @Tool(description = "Processes videos with different recipes like watermark and transcoding")
    public Map<String, Object> executeVideoRecipe(
            @ToolParam(description = "Input parameters for video processing") VideoRecipeInput input) {

        Map<String, Object> result = new HashMap<>();
        List<String> logs = new ArrayList<>();

        try {
            String fileLocation = input.getFileLocation();
            String recipeName = input.getRecipe().toLowerCase();

            VIDEO_RECIPE recipe = validateRecipeNames(recipeName);
            Map<String, Object> recipeParameters = input.getRecipeParameters();
            String outputFileFormat = tryParseString(recipeParameters, "outputFileFormat");

            log.info("Output File Format: {}", outputFileFormat);
            String fileExtension = !Strings.isNullOrEmpty(outputFileFormat) ? outputFileFormat : Files.getFileExtension(fileLocation);
            String outputFileName = "/tmp/" + UUID.randomUUID().toString() + "-" + recipe.name() + "." + fileExtension;

            log.info("Recipe Name: {}", recipe.name());
            log.info("Recipe Parameters: {}", recipeParameters);

            if (recipe == VIDEO_RECIPE.WATERMARK) {
                String watermarkFileLocation = ((String) recipeParameters.get("watermarkFileLocation"));
                watermark(fileLocation, watermarkFileLocation, outputFileName);
            } else if (recipe == VIDEO_RECIPE.TRANSCODE) {
                String videoEncoder = tryParseString(recipeParameters, "videoEncoder");
                Integer videoBitRate = tryParseInteger(recipeParameters, "videoBitRate");
                Integer frameRate = tryParseInteger(recipeParameters, "frameRate");
                String audioEncoder = tryParseString(recipeParameters, "audioEncoder");
                Integer audioBitRate = tryParseInteger(recipeParameters, "audioBitRate");
                Integer audioSamplingFrequency = tryParseInteger(recipeParameters, "audioSamplingFrequency");

                transcode(fileLocation,
                        videoEncoder,
                        videoBitRate,
                        frameRate,
                        audioEncoder,
                        audioBitRate,
                        audioSamplingFrequency,
                        outputFileName);
            }

            String s3BucketName = "image-processing-orkes";

            log.info("Uploading file to s3: {}", outputFileName);
            log.info("Uploading file size: {}", new File(outputFileName).length());

            String url = S3Utils.uploadToS3(outputFileName, Regions.US_EAST_1, s3BucketName);
            log.info("Completed File upload: {}", url);

            // Add outputs to result
            result.put("fileLocation", url);
            result.put("recipe", recipe);
            result.put("recipeParameters", recipeParameters);

        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());

            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw, true);
            e.printStackTrace(pw);
            String message = sw.getBuffer().toString();
            log.error(message);

            // Add error details to result
            result.put("error", message);
            throw new RuntimeException("Failed to execute video recipe: " + e.getMessage(), e);
        }

        return result;
    }

    private String tryParseString(Map<String, Object> obj, String key) {
        String value = null;
        if (obj.containsKey(key)) {
            value = ((String) obj.get(key));
        }
        return value;
    }

    private Integer tryParseInteger(Map<String, Object> obj, String key) {
        Integer value = null;
        if (obj.containsKey(key)) {
            value = Doubles.tryParse(obj.get(key).toString()).intValue();
        }
        return value;
    }

    private VIDEO_RECIPE validateRecipeNames(String recipeName) throws Exception {
        VIDEO_RECIPE recipe = VIDEO_RECIPE.fromString(recipeName);
        if (!supportedRecipes.contains(recipe)) {
            throw new Exception("Recipe: " + recipeName + " not supported. Supported recipes: " + supportedRecipes);
        }
        return recipe;
    }

    public void watermark(String inputFileLocation, String watermarkFileLocation, String outputFileLocation) throws Exception {

        String cmd = "ffmpeg -i " +
                inputFileLocation +
                " -i " +
                watermarkFileLocation +
                "  -filter_complex \"[1][0]scale2ref=w=oh*mdar:h=ih*0.1[logo][video];[video][logo]overlay=5:H-h-5\" " +
                " -c:a copy " +
                outputFileLocation;

        ProcessBuilder builder = new ProcessBuilder();
        builder.command("sh", "-c", cmd);

        log.info("ffmpeg cmd: {}", cmd);

        Process process = builder.start();
        String error = loadStream(process.getErrorStream());

        int rc = process.waitFor();
        if (rc != 0) {
            log.error("error message: {}", error);
            throw new Exception(error);
        }
    }

    public void transcode(String inputFileLocation,
                          String videoEncoder,
                          Integer videoBitRate,
                          Integer frameRate,
                          String audioEncoder,
                          Integer audioBitRate,
                          Integer audioSamplingFrequency,
                          String outputFileLocation
    ) throws Exception {

        String cmd = "ffmpeg -y -i " +
                inputFileLocation +
                (videoEncoder != null ? " -vcodec " + videoEncoder : "") +
                (videoBitRate != null ? " -b:v  " + videoBitRate : "") +
                (frameRate != null ? " -r  " + frameRate : "") +
                (audioEncoder != null ? " -acodec  " + audioEncoder : "") +
                (audioBitRate != null ? " -b:a  " + audioBitRate : "") +
                (audioSamplingFrequency != null ? " -ar  " + audioSamplingFrequency : "") +
                " " +
                outputFileLocation;

        ProcessBuilder builder = new ProcessBuilder();
        builder.command("sh", "-c", cmd);

        log.info("ffmpeg cmd: {}", cmd);

        Process process = builder.start();
        String output = loadStream(process.getInputStream());
        String error = loadStream(process.getErrorStream());

        int rc = process.waitFor();
        if (rc != 0) {
            log.error("error message: {}", error);
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