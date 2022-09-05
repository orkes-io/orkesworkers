package io.orkes.samples.workers;

import com.amazonaws.regions.Regions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import io.orkes.samples.utils.S3Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;


@Component
@Slf4j
public class GenerateThumbnailsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "generate_thumbnails";
    }

    @Override
    public TaskResult execute(Task task) {

        TaskResult result = new TaskResult(task);

        try {
            String fileLocation = (String) task.getInputData().get("fileLocation");
            String outputFileFormat = (String) task.getInputData().get("outputFileFormat");

            log.info("File Location: {}", fileLocation);
            log.info("Output File Format: {}", outputFileFormat);
            String fileExtension = outputFileFormat;
            String outputFileNamePrefix = "/tmp/" + UUID.randomUUID().toString();
            log.info("Output File Name Prefix: {}", outputFileNamePrefix);
            log.info("Output File Extension: {}", fileExtension);

            log.info("Retry count: {}", task.getRetryCount());


            List<ThumbnailInfo> thumbnailInfoList = thumbnailGenerate(fileLocation,
                    outputFileNamePrefix,
                    fileExtension);

            Map<String, Object> uploadInfo = uploadFiles(thumbnailInfoList);

            result.setStatus(TaskResult.Status.COMPLETED);
            result.addOutputData("thumbnailsInfo", uploadInfo);


        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            result.setStatus(TaskResult.Status.FAILED);
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw, true);
            e.printStackTrace(pw);
            String message = sw.getBuffer().toString();
            log.error(message);
            result.log(message);
        }
        return result;
    }



    public List<ThumbnailInfo> thumbnailGenerate(String inputFileLocation,
                                                  String  outputFileLocationPrefix,
                                                  String fileExtension
    )  throws  Exception {

        List<ThumbnailInfo> thumbnailInfos = Lists.newArrayList();

        String timeTxtFile = "/tmp/"+ UUID.randomUUID().toString() + "-time.txt";
        log.info("timeTxtFile: {}", timeTxtFile);

        String cmd = "ffmpeg -i " +
                inputFileLocation +
                " -vf " +
                " \"select='gt(scene\\,0.5)',metadata=print:file="+timeTxtFile+"\" " +
                " -vsync vfr "  +
                " " +
                outputFileLocationPrefix +"%05d"+
                "." + fileExtension ;

        ProcessBuilder builder = new ProcessBuilder();
        builder.command("sh", "-c", cmd);

        log.info("ffmpeg cmd: {}", cmd);

        Process process = builder.start();
        String output  = loadStream(process.getInputStream());
        String error  = loadStream(process.getErrorStream());

        int rc = process.waitFor();
        if(rc != 0) {
            log.error("error message: {}", error);
            throw new Exception(error);
        } else {
            thumbnailInfos = loadIndos(timeTxtFile, outputFileLocationPrefix, fileExtension);
        }
        return thumbnailInfos;
    }

    public static List<ThumbnailInfo> loadIndos(String timeTxtFile, String filenamePrefix, String fileExtension) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(timeTxtFile), StandardCharsets.UTF_8);
        List<ThumbnailInfo> thumbnailInfos = Lists.newArrayList();
        for (String line :
                lines) {
            if(line.startsWith("frame:")) {
                String[] tokens = line.split("\\s+");
                Long frameNum = Long.parseLong(tokens[0].split(":")[1]);
                Long pts = Long.parseLong(tokens[1].split(":")[1]);
                Double pts_time = Double.parseDouble(tokens[2].split(":")[1]);

                ThumbnailInfo thumbnailInfo = new ThumbnailInfo(String.format(filenamePrefix+"%05d"+"."+fileExtension,++frameNum),
                            pts,
                        pts_time
                        );
                thumbnailInfos.add(thumbnailInfo);
            }

        }
        return thumbnailInfos;
    }

    private Map<String, Object> uploadFiles(List<ThumbnailInfo> thumbnailInfos) {
        String s3BucketName = "image-processing-orkes";

        Map<String, Object> thumbnailUploadInfo = Maps.newHashMap();
        for (ThumbnailInfo thumbnailInfo:
             thumbnailInfos) {
            String url = S3Utils.uploadToS3(thumbnailInfo.getFileLocation(), Regions.US_EAST_1, s3BucketName);
            log.info("Completed File upload: {}", url);
            thumbnailUploadInfo.put(url, thumbnailInfo);

        }
        return thumbnailUploadInfo;
    }

    private static String loadStream(InputStream s) throws Exception
    {
        BufferedReader br = new BufferedReader(new InputStreamReader(s));
        StringBuilder sb = new StringBuilder();
        String line;
        while((line=br.readLine()) != null)
            sb.append(line).append("\n");
        return sb.toString();
    }
}
