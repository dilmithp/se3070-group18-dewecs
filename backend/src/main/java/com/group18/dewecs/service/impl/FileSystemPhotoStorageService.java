package com.group18.dewecs.service.impl;

import com.group18.dewecs.exception.GroundReportValidationException;
import com.group18.dewecs.exception.ResourceNotFoundException;
import com.group18.dewecs.service.PhotoStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class FileSystemPhotoStorageService implements PhotoStorageService {

    static final int MAX_BYTES = 5 * 1024 * 1024;
    private static final Pattern GENERATED_NAME = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp)$");
    private static final Logger log = LoggerFactory.getLogger(FileSystemPhotoStorageService.class);

    private final Path root;

    public FileSystemPhotoStorageService(@Value("${dewecs.photos.dir:uploads/ground-reports}") String directory) {
        this.root = Paths.get(directory).toAbsolutePath().normalize();
    }

    @Override
    public String store(byte[] content) {
        if (content == null || content.length == 0) {
            throw new GroundReportValidationException("The photo is empty.");
        }
        if (content.length > MAX_BYTES) {
            throw new GroundReportValidationException("The photo is larger than 5 MB.");
        }
        String extension = detectExtension(content)
                .orElseThrow(() -> new GroundReportValidationException("The photo must be a JPEG, PNG or WebP image."));
        String name = UUID.randomUUID() + "." + extension;
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
        if (name == null || !GENERATED_NAME.matcher(name).matches()) {
            return Optional.empty();
        }
        Path file = root.resolve(name).normalize();
        return file.startsWith(root) ? Optional.of(file) : Optional.empty();
    }

    private Optional<String> detectExtension(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return Optional.of("jpg");
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return Optional.of("png");
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return Optional.of("webp");
        }
        return Optional.empty();
    }
}
