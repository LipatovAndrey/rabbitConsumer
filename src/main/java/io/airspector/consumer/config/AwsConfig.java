package io.airspector.config;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AwsConfig {

    @Bean
    public AmazonS3 s3Client() {
        return AmazonS3ClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials()))
                .withRegion(Regions.EU_CENTRAL_1)
                .build();
    }

    @Bean
    public AWSCredentials awsCredentials() {
        return new BasicAWSCredentials("AKIA4J6BVAGCBJMT6X4R", "e4SM2vcCjm9ykwhir6ZejjMhJ9vWoQnKdKu8J6un");
    }
}
