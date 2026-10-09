package com.group18.dewecs.service;

import com.group18.dewecs.exception.GroundReportValidationException;
import com.group18.dewecs.exception.ResourceNotFoundException;
import com.group18.dewecs.service.impl.S3PhotoStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3PhotoStorageServiceTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};
    private static final String NAME = "123e4567-e89b-12d3-a456-426614174000.png";

    @Mock
    private S3Client s3;

    private S3PhotoStorageService storage;

    @BeforeEach
    void setUp() {
        storage = new S3PhotoStorageService(s3, "dev-bucket-syn", "ground-reports/");
    }

    @Test
    void store_uploadsUnderTheGeneratedNameWithTheRightContentType() {
        String name = storage.store(PNG);

        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3).putObject(request.capture(), any(RequestBody.class));
        assertThat(name).matches("[0-9a-f-]{36}\\.png");
        assertThat(request.getValue().bucket()).isEqualTo("dev-bucket-syn");
        assertThat(request.getValue().key()).isEqualTo("ground-reports/" + name);
        assertThat(request.getValue().contentType()).isEqualTo("image/png");
    }

    @Test
    void store_rejectsNonImagesWithoutCallingS3() {
        assertThatThrownBy(() -> storage.store("not an image".getBytes()))
                .isInstanceOf(GroundReportValidationException.class);
        assertThatThrownBy(() -> storage.store(new byte[0])).isInstanceOf(GroundReportValidationException.class);

        verifyNoInteractions(s3);
    }

    @Test
    @SuppressWarnings("unchecked")
    void load_returnsTheObjectBytes() throws IOException {
        ResponseBytes<GetObjectResponse> bytes = ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), PNG);
        when(s3.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(bytes);

        Resource resource = storage.load(NAME);

        assertThat(resource.getContentAsByteArray()).isEqualTo(PNG);
        ArgumentCaptor<GetObjectRequest> request = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3).getObjectAsBytes(request.capture());
        assertThat(request.getValue().key()).isEqualTo("ground-reports/" + NAME);
    }

    @Test
    void load_missingObjectIsNotFound() {
        when(s3.getObjectAsBytes(any(GetObjectRequest.class))).thenThrow(NoSuchKeyException.builder().build());

        assertThatThrownBy(() -> storage.load(NAME)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void load_andDelete_ignoreNamesThatWereNotGeneratedByTheServer() {
        assertThatThrownBy(() -> storage.load("../secret.png")).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> storage.load(null)).isInstanceOf(ResourceNotFoundException.class);
        storage.delete("../secret.png");

        verifyNoInteractions(s3);
    }

    @Test
    void delete_removesTheObjectAndNeverThrows() {
        storage.delete(NAME);
        ArgumentCaptor<DeleteObjectRequest> request = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3).deleteObject(request.capture());
        assertThat(request.getValue().key()).isEqualTo("ground-reports/" + NAME);

        when(s3.deleteObject(any(DeleteObjectRequest.class))).thenThrow(new RuntimeException("boom"));
        assertThatCode(() -> storage.delete(NAME)).doesNotThrowAnyException();
        verify(s3, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }
}
