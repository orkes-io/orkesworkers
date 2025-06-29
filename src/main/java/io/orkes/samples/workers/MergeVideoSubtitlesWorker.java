package io.orkes.samples.workers;

import com.amazonaws.regions.Regions;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import io.orkes.samples.utils.S3Utils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class MergeVideoSubtitlesWorker {

    @Data
    public static class MergeVideoSubtitlesInput {
        private List<Map<String, Object>> files_to_merge;
        private String outputFileFormat;
        private String fileLocation;
    }

    @WorkerTask("merge_video_subtitles")
    @Tool(description = "Merges video with subtitles from multiple files")
    public Map<String, Object> mergeVideoSubtitles(
            @ToolParam(description = "Input parameters for merging videos and subtitles") MergeVideoSubtitlesInput input) {

        Map<String, Object> result = new HashMap<>();
        List<String> tmpFiles = Lists.newArrayList();

        try {
            String outputFileFormat = input.getOutputFileFormat();
            String fileLocation = input.getFileLocation();
            String outputSubtitleFileFormat = "srt";
            String inputFilesListContent = "";
            String subtitllesFileListContent = "";

            // Download original File that needs to be subtitled
            String originalFileName = Files.getNameWithoutExtension(fileLocation) + "." + Files.getFileExtension(fileLocation);
            InputStream originalFileNameStream = new URL(fileLocation).openStream();
            String tmpOriginalFileName = "/tmp/" + UUID.randomUUID().toString() + "-" + originalFileName;
            java.nio.file.Files.copy(originalFileNameStream, Paths.get(tmpOriginalFileName), StandardCopyOption.REPLACE_EXISTING);
            tmpFiles.add(tmpOriginalFileName);

            // Generate the metadata files for the split media file and split subtitles
            for (Map<String, Object> fileInfo : input.getFiles_to_merge()) {
                String videoFileUrl = (String) fileInfo.get("videoFileWithSubtitlesUrl");
                String subtitleFileUrl = (String) fileInfo.get("subtitleFileUrl");

                String videoFileName = Files.getFileExtension(videoFileUrl) + "." + Files.getNameWithoutExtension(videoFileUrl);
                String subTitleFileName = Files.getFileExtension(subtitleFileUrl) + "." + Files.getNameWithoutExtension(subtitleFileUrl);

                InputStream videoFileNameStream = new URL(videoFileUrl).openStream();
                String tmpVideoFileName = "/tmp/" + UUID.randomUUID().toString() + "-" + videoFileName;
                java.nio.file.Files.copy(videoFileNameStream, Paths.get(tmpVideoFileName), StandardCopyOption.REPLACE_EXISTING);
                tmpFiles.add(tmpVideoFileName);
                inputFilesListContent = inputFilesListContent + "file " + tmpVideoFileName + System.lineSeparator();

                InputStream subtitleFileNameStream = new URL(subtitleFileUrl).openStream();
                String subtitleFileName = "/tmp/" + UUID.randomUUID().toString() + "-" + subTitleFileName;
                java.nio.file.Files.copy(subtitleFileNameStream, Paths.get(subtitleFileName), StandardCopyOption.REPLACE_EXISTING);
                tmpFiles.add(subtitleFileName);
                subtitllesFileListContent = subtitllesFileListContent + subtitleFileName + System.lineSeparator();
            }

            // Save metadata to file
            String inputFilesListName = "/tmp/" + UUID.randomUUID().toString() + "-input.txt";
            String subtitlesFileListName = "/tmp/" + UUID.randomUUID().toString() + "-subtitles.txt";

            FileUtils.writeStringToFile(new File(inputFilesListName), inputFilesListContent, Charset.forName("UTF-8"));
            FileUtils.writeStringToFile(new File(subtitlesFileListName), subtitllesFileListContent, Charset.forName("UTF-8"));
            tmpFiles.add(inputFilesListName);
            tmpFiles.add(subtitlesFileListName);

            String mergedFileName = "/tmp/" + UUID.randomUUID().toString() + "." + outputFileFormat;
            // Merge split videos and subtitles into one file
            mergeVideos(inputFilesListName, subtitlesFileListName, mergedFileName);
            tmpFiles.add(mergedFileName);

            // Extract the merged subtitle file out
            String outputMergedSubtitleFile = "/tmp/" + UUID.randomUUID().toString() + "." + outputSubtitleFileFormat;
            extractMergedSub(mergedFileName, outputMergedSubtitleFile);
            tmpFiles.add(outputMergedSubtitleFile);

            // add the subtitle file to the original video
            String outputFileName = "/tmp/" + UUID.randomUUID().toString() + "." + outputFileFormat;
            addMergedSubToVideo(tmpOriginalFileName, outputMergedSubtitleFile, outputFileName);
            tmpFiles.add(outputFileName);

            String s3BucketName = "image-processing-orkes";

            log.info("Uploading media file to s3: {}", outputFileName);
            log.info("Uploading media file size: {}", new File(outputFileName).length());

            String url = S3Utils.uploadToS3(outputFileName, Regions.US_EAST_1, s3BucketName);
            log.info("Completed media File upload: {}", url);

            log.info("Uploading subtitle file to s3: {}", outputFileName);
            log.info("Uploading subtitlefile size: {}", new File(outputFileName).length());

            String subTitleUrl = S3Utils.uploadToS3(outputMergedSubtitleFile, Regions.US_EAST_1, s3BucketName);
            log.info("Completed subtitle File upload: {}", subTitleUrl);

            result.put("fileLocation", url);
            result.put("subTitleFileLocation", subTitleUrl);

            // Clean up temporary files
            for (String file : tmpFiles) {
                try {
                    java.nio.file.Files.deleteIfExists(Paths.get(file));
                } catch (Exception e) {
                    // Add file cleanup errors to result
                    result.put("cleanupError_" + file, e.getMessage());
                }
            }

            return result;

        } catch (Exception e) {
            log.error(e.getMessage());
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw, true);
            e.printStackTrace(pw);
            String message = sw.getBuffer().toString();
            log.error(message);

            // Clean up temporary files even on error
            for (String file : tmpFiles) {
                try {
                    java.nio.file.Files.deleteIfExists(Paths.get(file));
                } catch (Exception cleanupEx) {
                    log.error("Error cleaning up file: " + file, cleanupEx);
                }
            }

            // Add error info to result
            result.put("error", e.getMessage());
            result.put("stackTrace", message);
            throw new RuntimeException("Failed to merge video and subtitles: " + e.getMessage(), e);
        }
    }

    public void addMergedSubToVideo(String originalFileName, String outputMergedSubtitleFile, String outputFileName) throws Exception {
        // ffmpeg -i input.mp4 -i input.srt -map 0 -map 1 -c:v copy -c:a copy -c:s mov_text output.m4v
        String cmd = "ffmpeg -i " +
                originalFileName +
                "  -i " +
                outputMergedSubtitleFile +
                "  -map 0 -map 1 -c:v copy -c:a copy -c:s mov_text   " +
                outputFileName;

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

    public void extractMergedSub(String mergedFileName, String outputMergedSubtitleFile) throws Exception {
        // ffmpeg -i output.m4v -map 0:s:0 output.srt
        String cmd = "ffmpeg -i " +
                mergedFileName +
                "  -map 0:s:0  " +
                outputMergedSubtitleFile;

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

    public void mergeVideos(String inputFilesListName, String subtitlesFileListName, String outputFileName) throws Exception {
        // ffmpeg -f concat -safe 0 -i input.txt -f srt -i subtitles.txt -c copy -c:s mov_text output.m4v
        String cmd = "ffmpeg -f concat -safe 0 -i " +
                inputFilesListName +
                " -f srt -i " +
                subtitlesFileListName +
                " -c copy -c:s mov_text   " +
                outputFileName;

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

    private static String loadStream(InputStream s) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(s));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null)
            sb.append(line).append("\n");
        return sb.toString();
    }
}