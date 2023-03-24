package io.orkes.samples.workers;

import com.amazonaws.regions.Regions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Doubles;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import io.orkes.samples.utils.S3Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.PrefixFileFilter;
@Component
@Slf4j
public class SplitVideoWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "split_video";
    }

    @Override
    public TaskResult execute(Task task) {

        TaskResult result = new TaskResult(task);

        try {
            String fileLocation = (String) task.getInputData().get("fileLocation");
            String outputFileNamePrefix = (String) task.getInputData().get("outputFileNamePrefix");
            Integer durationInSeconds = Doubles.tryParse(task.getInputData().get("durationInSeconds").toString()).intValue();

            Path tmpOutputdir = java.nio.file.Files.createTempDirectory(Paths.get("/tmp"), "split-video-");
            Path tmpOutputdirPrefix = Paths.get(tmpOutputdir.toString(),outputFileNamePrefix);

            InputStream in = new URL(fileLocation).openStream();
            String fileExtension = com.google.common.io.Files.getFileExtension(fileLocation);

            String tmpInputFileName = "/tmp/" + UUID.randomUUID().toString() + "."+fileExtension;
            java.nio.file.Files.copy(in, Paths.get(tmpInputFileName), StandardCopyOption.REPLACE_EXISTING);

            splitVideo(tmpInputFileName, durationInSeconds, tmpOutputdirPrefix.toString(), fileExtension);

            List<String> urls = uploadFiles(tmpOutputdir, outputFileNamePrefix);

            Map<String, Object> splitFiles = Maps.newHashMap();
            int i = 0;
            for (String url:
                 urls) {
                splitFiles.put(i++ + "", url);
            }

            try {
                    Files.delete(Paths.get(tmpInputFileName));
                    Files.delete(tmpOutputdir);
            } catch (Exception e) {

            }

            result.setStatus(TaskResult.Status.COMPLETED);
            result.addOutputData("fileLocation", fileLocation);
            result.addOutputData("splitFiles", splitFiles);


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


    private List<String> uploadFiles(Path path, String fileNamePrefix) {
        List<File> files = Lists.newArrayList(path.toFile().listFiles((FileFilter) new PrefixFileFilter(fileNamePrefix, IOCase.SENSITIVE)));
        String s3BucketName = "image-processing-orkes";
        List<String> urls = Lists.newArrayList();
        for (File file:
                files.stream().sorted().collect(Collectors.toList())) {

            log.info("Uploading file to s3: {}", file.getAbsoluteFile().toString());
            log.info("Uploading file size: {}", file.length());

            String url = S3Utils.uploadToS3(file.getAbsolutePath(), Regions.US_EAST_1, s3BucketName);
            log.info("Completed File upload: {}", url);
            urls.add(url);
        }
        return urls;
    }

    public void splitVideo(String inputFileLocation, Integer durationInSeconds, String  outputFileNamePrefix, String outputFileExtension )  throws  Exception {

        // ffmpeg -i netflix.mp4 -c copy -map 0 -segment_time 30 -f segment -reset_timestamps 1 output%03d.mp4
        String cmd = "ffmpeg -i " +
                        inputFileLocation +
                        " -c copy -map 0 -segment_time " +
                        durationInSeconds +
                        " -f segment -reset_timestamps 1  " +
                        outputFileNamePrefix +
                        "%03d" + "." + outputFileExtension ;
                ;

        ProcessBuilder builder = new ProcessBuilder();
        builder.command("sh", "-c", cmd);

        log.info("ffmpeg cmd: {}", cmd);

        Process process = builder.start();
        String error  = loadStream(process.getErrorStream());

        int rc = process.waitFor();
        if(rc != 0) {
            log.error("error message: {}", error);
            throw new Exception(error);
        }
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
