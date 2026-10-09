package com.group18.dewecs.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * The S3 client, only when dewecs.photos.storage=s3. Keys come from dewecs.photos.s3.access-key / secret-key (set in
 * the git-ignored config/application-prod.properties); when those are empty the AWS default chain is used
 * (AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY environment variables). No key appears in code or tracked files.
 */
@Configuration
@ConditionalOnProperty(name = "dewecs.photos.storage", havingValue = "s3")
public class S3Config {

    @Bean(destroyMethod = "close")
    public S3Client s3Client(@Value("${dewecs.photos.s3.region}") String region,
                              @Value("${dewecs.photos.s3.access-key:}") String accessKey,
                              @Value("${dewecs.photos.s3.secret-key:}") String secretKey) {
        var builder = S3Client.builder().region(Region.of(region));
        if (!accessKey.isBlank() && !secretKey.isBlank()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)));
        }
        return builder.build();
    }
}
