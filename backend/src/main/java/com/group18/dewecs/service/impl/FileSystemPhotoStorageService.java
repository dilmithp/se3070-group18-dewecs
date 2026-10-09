package com.group18.dewecs.service.impl;

import com.group18.dewecs.exception.ResourceNotFoundException;
import com.group18.dewecs.service.PhotoStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

/** Photos in a local folder (default; used by tests and the local demo). Set dewecs.photos.storage=s3 for S3. */
@Service
@ConditionalOnProperty(name = "dewecs.photos.storage", havingValue = "filesystem", matchIfMissing = true)
public class FileSystemPhotoStorageService implements PhotoStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileSystemPhotoStorageService.class);

    private final Path root;

    public FileSystemPhotoStorageService(@Value("${dewecs.photos.dir:uploads/ground-reports}") String directory) {
        this.root = Paths.get(directory).toAbsolutePath().normalize();
    }

    @Override
    public String store(byte[] content) {
        String name = PhotoContent.newName(content);
        try {
            Files.createDirectories(root);
            Files.write(root.resolve(name), content, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store the photo", e);
        }
        return name;
    }

    @Override
    public Resource load(String name) {
        Path file = resolve(name).orElseThrow(() -> new ResourceNotFoundException("Photo not found"));
        if (!Files.isRegularFile(file)) {
            throw new ResourceNotFoundException("Photo not found");
        }
        return new FileSystemResource(file);
    }

    @Override
    public void delete(String name) {
        try {
            resolve(name).ifPresent(file -> {
                try {
                    Files.deleteIfExists(file);
                } catch (IOException e) {
                    log.warn("Could not delete photo file {}", name);
                }
            });
        } catch (RuntimeException e) {
            log.warn("Could not delete photo file {}", name);
        }
    }

    private Optional<Path> resolve(String name) {
        if (!PhotoContent.isGeneratedName(name)) {
            return Optional.empty();
        }
        Path file = root.resolve(name).normalize();
        return file.startsWith(root) ? Optional.of(file) : Optional.empty();
    }
}
