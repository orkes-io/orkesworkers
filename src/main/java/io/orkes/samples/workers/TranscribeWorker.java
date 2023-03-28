package io.orkes.samples.workers;

import com.amazonaws.regions.Regions;
import com.google.common.primitives.Doubles;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import io.orkes.samples.utils.S3Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Component;
import com.google.common.base.Strings;
import com.google.common.io.Files;

import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.UUID;

@Component
@Slf4j
public class TranscribeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "transcribe";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);

        try {

            String fileLocation = (String) task.getInputData().get("fileLocation");
            String openApiKey = (String) task.getInputData().get("open_api_key");
            Integer dataIndex = Doubles.tryParse(task.getInputData().get("dataIndex").toString()).intValue();
            result.log("dataIndex: " + dataIndex);


            HttpPost post = new HttpPost("https://api.openai.com/v1/audio/transcriptions");
            post.addHeader("Authorization", "Bearer " + openApiKey);

            InputStream in = new URL(fileLocation).openStream();
            String fileExtension = Files.getFileExtension(fileLocation);
            String tmpInputFileName = "/tmp/" + UUID.randomUUID().toString() + "."+fileExtension;
            java.nio.file.Files.copy(in, Paths.get(tmpInputFileName), StandardCopyOption.REPLACE_EXISTING);

            final File file = new File(tmpInputFileName);
            final MultipartEntityBuilder builder = MultipartEntityBuilder.create();

            FileBody data = new FileBody(file);
            String responseFormat = "srt";
            builder.addPart("model", new StringBody("whisper-1", ContentType.DEFAULT_TEXT));
            builder.addPart("response_format", new StringBody(responseFormat, ContentType.DEFAULT_TEXT));
            builder.addPart("file", data);

            final HttpEntity entity = builder.build();
            post.setEntity(entity);

            try (CloseableHttpClient client = HttpClientBuilder.create()
                    .build()) {
                try (CloseableHttpResponse response = (CloseableHttpResponse) client
                        .execute(post)) {
                    HttpEntity responseEntity = response.getEntity();
                    String content = EntityUtils.toString(response.getEntity());
                    EntityUtils.consume(responseEntity);

                    if(response.getStatusLine().getStatusCode() != 200) {
                        throw new Exception(content);
                    }
                    // TODO: check to see what content holds..  it needs to be a valid file,
                    // if its empty .. put into a empty file here
                    // if it has json content.. the error message is written into the srt file..
                    // this needs to be handled and put into a error message
                    // Is there a better way of handling this

                    String nameWithoutExtension = Files.getNameWithoutExtension(fileLocation);
                    String tmpOutputFileName = "/tmp/" + UUID.randomUUID().toString() + "-" + nameWithoutExtension + "."+ responseFormat;
                    Path path = Paths.get(tmpOutputFileName);

                    byte[] strToBytes = content.getBytes();
                    java.nio.file.Files.write(path, strToBytes);

                    String outputFileNameWithSubtitles = nameWithoutExtension+".m4v";
                    String outputFileWithSubtitles = "/tmp/" + UUID.randomUUID().toString() + "-" + outputFileNameWithSubtitles;

                    addSubtitleToFile(tmpInputFileName, tmpOutputFileName, "eng", outputFileWithSubtitles);

                    String s3BucketName = "image-processing-orkes";
                    String subtitleFileUrl = S3Utils.uploadToS3(tmpOutputFileName, Regions.US_EAST_1, s3BucketName);
                    String videoFileWithSubtitlesUrl = S3Utils.uploadToS3(outputFileWithSubtitles, Regions.US_EAST_1, s3BucketName);
                    result.log("file Uploaded: " + subtitleFileUrl);
                    result.log("file Uploaded: " + videoFileWithSubtitlesUrl);

                    result.addOutputData("subtitleFileUrl", subtitleFileUrl);
                    result.addOutputData("videoFileWithSubtitlesUrl", videoFileWithSubtitlesUrl);
                    result.addOutputData("dataIndex", dataIndex);
                    result.addOutputData("fileLocation", fileLocation);
                    result.log("dataIndex: " + dataIndex);

                }
            }


            result.setStatus(TaskResult.Status.COMPLETED);

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


    public void addSubtitleToFile(String inputFileLocation, String subtitleLocation, String language,  String outputFile )  throws  Exception {

        //        ffmpeg -i ./$f -i $f.srt -c copy -c:s mov_text -metadata:s:s:0 language=eng $f.m4v
        String cmd = "ffmpeg -i " +
                inputFileLocation +
                " -i " +
                subtitleLocation +
                " -c copy -c:s mov_text -metadata:s:s:0 language=" +
                language +
                " " +
                outputFile
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
