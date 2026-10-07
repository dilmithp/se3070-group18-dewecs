package com.group18.dewecs.service;

import org.springframework.core.io.Resource;

public interface PhotoStorageService {

    /** Path prefix of every stored photo as seen by API clients. */
    String URL_PREFIX = "/api/v1/photos/";

    /**
     * Validates by content (JPEG, PNG or WebP, 1 byte to 5 MB), stores the bytes under a server-generated name
     * and returns that name. The client's file name and Content-Type are never used.
     */
    String store(byte[] content);

    /** Returns the stored file, or throws ResourceNotFoundException for unknown or malformed names. */
    Resource load(String name);

    /** Best effort; applies the same name and containment rules as {@link #load(String)}. */
    void delete(String name);
}
