package com.group18.dewecs.service.impl;

import com.group18.dewecs.exception.ResourceNotFoundException;
import com.group18.dewecs.service.PhotoStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Photos in an S3 bucket (dewecs.photos.storage=s3). Same rules and the same /api/v1/photos/{name} URLs as the
 * local store: the bucket is never exposed, the app streams the bytes. Credentials come from the standard AWS
 * environment variables, never from files in the repository.
 */
@Service
@ConditionalOnProperty(name = "dewecs.photos.storage", havingValue = "s3")
public class S3PhotoStorageService implements PhotoStorageService {

    private static final Logger log = LoggerFactory.getLogger(S3PhotoStorageService.class);

    private final S3Client s3;
    private final String bucket;
    private final String prefix;

    public S3PhotoStorageService(S3Client s3,
                                 @Value("${dewecs.photos.s3.bucket}") String bucket,
                                 @Value("${dewecs.photos.s3.prefix:ground-reports/}") String prefix) {
        this.s3 = s3;
        this.bucket = bucket;
        this.prefix = prefix;
    }

    @Override
    public String store(byte[] content) {
        String name = PhotoContent.newName(content);
        s3.putObject(PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(prefix + name)
                        .contentType(contentTypeOf(name))
                        .build(),
                RequestBody.fromBytes(content));
        return name;
    }

    @Override
    public Resource load(String name) {
        if (!PhotoContent.isGeneratedName(name)) {
            throw new ResourceNotFoundException("Photo not found");
        }
        try {
            byte[] bytes = s3.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(prefix + name).build())
                    .asByteArray();
            return new ByteArrayResource(bytes);
        } catch (NoSuchKeyException e) {
            throw new ResourceNotFoundException("Photo not found");
        }
    }

    @Override
    public void delete(String name) {
        if (!PhotoContent.isGeneratedName(name)) {
            return;
        }
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(prefix + name).build());
        } catch (RuntimeException e) {
            log.warn("Could not delete photo object {}", name);
        }
    }

    private String contentTypeOf(String name) {
        if (name.endsWith(".png")) {
            return "image/png";
        }
        return name.endsWith(".webp") ? "image/webp" : "image/jpeg";
    }
}
