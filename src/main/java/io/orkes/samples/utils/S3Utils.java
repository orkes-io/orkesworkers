package io.orkes.samples.utils;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.WebIdentityTokenCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;

@Slf4j
public class S3Utils {

    public static String uploadToS3(String fileName, Regions region, String bucketName) {
        String stringObjKeyName = Paths.get(fileName).getFileName().toString();
        URL url = null;
        try {
            // This code expects that you have AWS credentials set up per:
            // https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/setup-credentials.html

            AWSCredentialsProviderChain awsCredentialsProvider = new AWSCredentialsProviderChain(WebIdentityTokenCredentialsProvider.create());
            AmazonS3 s3Client = AmazonS3ClientBuilder
                    .standard()
                    .withRegion(region)
                    .withCredentials(awsCredentialsProvider)
                    .build();

            // Upload a file as a new object.
            s3Client.putObject(bucketName, stringObjKeyName, new File(fileName));
            url = s3Client.getUrl(bucketName, stringObjKeyName);
        } catch (AmazonServiceException e) {
            // The call was transmitted successfully, but Amazon S3 couldn't process
            // it, so it returned an error response.
            log.error("Error while uploading to S3", e);
            throw e;
        } catch (SdkClientException e) {
            // Amazon S3 couldn't be contacted for a response, or the client
            // couldn't parse the response from Amazon S3.
            log.error("Error while uploading to S3", e);
            throw e;
        }

        return url.toString();
    }

}
