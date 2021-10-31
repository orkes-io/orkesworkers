package io.orkes.samples.utils;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.nio.file.Paths;


@Log4j2
public class S3Utils {

    public static void uploadtToS3(String fileName, Regions region, String bucketName) {
        String stringObjKeyName = Paths.get(fileName).getFileName().toString();
        try {
            // This code expects that you have AWS credentials set up per:
            // https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/setup-credentials.html
            AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                    .withCredentials(new ProfileCredentialsProvider("orkes-workers"))
                    .withRegion(region)
                    .build();

            // Upload a file as a new object.
            s3Client.putObject(bucketName, stringObjKeyName, new File(fileName));

        } catch (AmazonServiceException e) {
            // The call was transmitted successfully, but Amazon S3 couldn't process
            // it, so it returned an error response.
            log.info(e);
            throw e;
        } catch (SdkClientException e) {
            // Amazon S3 couldn't be contacted for a response, or the client
            // couldn't parse the response from Amazon S3.
            log.info(e);
            throw e;
        }
    }
}
