package com.group18.dewecs.service.impl;

import com.group18.dewecs.exception.GroundReportValidationException;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/** Content rules shared by every photo store: size, real image type, and the server-generated file name. */
final class PhotoContent {

    static final int MAX_BYTES = 5 * 1024 * 1024;
    private static final Pattern GENERATED_NAME = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp)$");

    private PhotoContent() {
    }

    /** Validates the bytes by content and returns a new server-generated name such as {uuid}.png. */
    static String newName(byte[] content) {
        if (content == null || content.length == 0) {
            throw new GroundReportValidationException("The photo is empty.");
        }
        if (content.length > MAX_BYTES) {
            throw new GroundReportValidationException("The photo is larger than 5 MB.");
        }
        String extension = detectExtension(content)
                .orElseThrow(() -> new GroundReportValidationException("The photo must be a JPEG, PNG or WebP image."));
        return UUID.randomUUID() + "." + extension;
    }

    static boolean isGeneratedName(String name) {
        return name != null && GENERATED_NAME.matcher(name).matches();
    }

    private static Optional<String> detectExtension(byte[] b) {
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
