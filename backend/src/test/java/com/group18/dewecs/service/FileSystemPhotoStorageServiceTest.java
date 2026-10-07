package com.group18.dewecs.service;

import com.group18.dewecs.exception.GroundReportValidationException;
import com.group18.dewecs.exception.ResourceNotFoundException;
import com.group18.dewecs.service.impl.FileSystemPhotoStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileSystemPhotoStorageServiceTest {

    private static final Pattern GENERATED = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp)$");

    @TempDir
    Path tempDir;

    private Path photoDir;
    private FileSystemPhotoStorageService storage;

    @BeforeEach
    void setUp() {
        photoDir = tempDir.resolve("photos");
        storage = new FileSystemPhotoStorageService(photoDir.toString());
    }

    private static byte[] jpeg() {
        return new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2};
    }

    private static byte[] png() {
        return new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0};
    }

    private static byte[] webp() {
        return new byte[] {'R', 'I', 'F', 'F', 1, 2, 3, 4, 'W', 'E', 'B', 'P', 0};
    }

    @Test
    void store_acceptsJpegPngAndWebpAndNamesThemByContent() throws IOException {
        String jpg = storage.store(jpeg());
        String png = storage.store(png());
        String webp = storage.store(webp());

        assertThat(jpg).matches(GENERATED).endsWith(".jpg");
        assertThat(png).matches(GENERATED).endsWith(".png");
        assertThat(webp).matches(GENERATED).endsWith(".webp");
        assertThat(Files.readAllBytes(photoDir.resolve(png))).isEqualTo(png());
    }

    @Test
    void store_createsTheDirectoryOnFirstUseAndNeverReusesANames() {
        assertThat(photoDir).doesNotExist();

        String first = storage.store(png());
        String second = storage.store(png());

        assertThat(photoDir).isDirectory();
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void store_rejectsATextFileEvenIfItWouldBeCalledJpg() {
        byte[] text = "this is not an image".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> storage.store(text)).isInstanceOf(GroundReportValidationException.class);
    }

    @Test
    void store_rejectsEmptyAndNull() {
        assertThatThrownBy(() -> storage.store(new byte[0])).isInstanceOf(GroundReportValidationException.class);
        assertThatThrownBy(() -> storage.store(null)).isInstanceOf(GroundReportValidationException.class);
    }

    @Test
    void store_rejectsMoreThanFiveMegabytes_butAcceptsExactlyFive() {
        byte[] tooBig = new byte[5 * 1024 * 1024 + 1];
        System.arraycopy(png(), 0, tooBig, 0, 8);
        byte[] exactly = new byte[5 * 1024 * 1024];
        System.arraycopy(png(), 0, exactly, 0, 8);

        assertThatThrownBy(() -> storage.store(tooBig)).isInstanceOf(GroundReportValidationException.class);
        assertThat(storage.store(exactly)).endsWith(".png");
    }

    @Test
    void store_rejectsTruncatedMagicBytes() {
        assertThatThrownBy(() -> storage.store(new byte[] {(byte) 0xFF, (byte) 0xD8}))
                .isInstanceOf(GroundReportValidationException.class);
        assertThatThrownBy(() -> storage.store(new byte[] {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'A', 'V', 'E'}))
                .isInstanceOf(GroundReportValidationException.class);
    }

    @Test
    void load_returnsTheStoredBytes() throws IOException {
        String name = storage.store(png());

        assertThat(storage.load(name).getContentAsByteArray()).isEqualTo(png());
    }

    @Test
    void load_unknownButWellFormedNameIsNotFound() {
        assertThatThrownBy(() -> storage.load("123e4567-e89b-12d3-a456-426614174000.png"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void load_andDelete_refuseTraversalAndNonGeneratedNames() throws IOException {
        Files.createDirectories(photoDir);
        Path secret = tempDir.resolve("secret.txt");
        Files.writeString(secret, "secret");
        Files.writeString(photoDir.resolve("notes.txt"), "plain file inside the directory");

        for (String bad : new String[] {"../secret.txt", "..\\secret.txt", secret.toString(), "/etc/passwd",
                "C:\\Windows\\win.ini", "notes.txt", "", " ", "123e4567-e89b-12d3-a456-426614174000.gif",
                "123E4567-E89B-12D3-A456-426614174000.png", "../123e4567-e89b-12d3-a456-426614174000.png",
                "123e4567-e89b-12d3-a456-426614174000.png/..", null}) {
            assertThatThrownBy(() -> storage.load(bad)).as("load %s", bad).isInstanceOf(ResourceNotFoundException.class);
            storage.delete(bad);
        }

        assertThat(secret).exists();
        assertThat(photoDir.resolve("notes.txt")).exists();
    }

    @Test
    void delete_removesAStoredFile_andIsQuietForMissingOnes() {
        String name = storage.store(png());

        storage.delete(name);
        storage.delete(name);
        storage.delete("123e4567-e89b-12d3-a456-426614174000.png");

        assertThat(photoDir.resolve(name)).doesNotExist();
    }
}
