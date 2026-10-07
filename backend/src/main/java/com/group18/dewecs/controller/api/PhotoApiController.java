package com.group18.dewecs.controller.api;

import com.group18.dewecs.service.PhotoStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/photos")
public class PhotoApiController {

    private final PhotoStorageService photoStorage;

    public PhotoApiController(PhotoStorageService photoStorage) {
        this.photoStorage = photoStorage;
    }

    @GetMapping("/{filename}")
    public ResponseEntity<Resource> photo(@PathVariable String filename) {
        Resource resource = photoStorage.load(filename);
        return ResponseEntity.ok()
                .contentType(mediaTypeOf(filename))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic())
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }

    private MediaType mediaTypeOf(String filename) {
        if (filename.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (filename.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        return MediaType.IMAGE_JPEG;
    }
}
