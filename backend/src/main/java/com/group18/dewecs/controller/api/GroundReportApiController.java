package com.group18.dewecs.controller.api;

import com.group18.dewecs.dto.api.CreateGroundReportRequest;
import com.group18.dewecs.dto.api.ReportResponse;
import com.group18.dewecs.mapper.GroundReportApiMapper;
import com.group18.dewecs.service.GroundReportService;
import com.group18.dewecs.service.GroundReportSubmissionService;
import com.group18.dewecs.service.GroundReportSubmissionService.SubmissionResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;

@RestController
@RequestMapping("/api/v1/ground-reports")
public class GroundReportApiController {

    private final GroundReportSubmissionService submissionService;
    private final GroundReportService groundReportService;
    private final GroundReportApiMapper mapper;

    public GroundReportApiController(GroundReportSubmissionService submissionService,
                                     GroundReportService groundReportService, GroundReportApiMapper mapper) {
        this.submissionService = submissionService;
        this.groundReportService = groundReportService;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<ReportResponse> submit(@Valid @RequestBody CreateGroundReportRequest request) {
        SubmissionResult result = submissionService.submit(request.citizenId(), request.districtId(),
                request.category(), request.description(), request.gpsLat(), request.gpsLng(), request.capturedAt());
        ReportResponse body = mapper.toReport(result.report());
        if (!result.created()) {
            return ResponseEntity.ok(body);
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/v1/ground-reports/" + body.id()))
                .body(body);
    }

    @PostMapping(path = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ReportResponse uploadPhoto(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
        try {
            return mapper.toReport(submissionService.attachPhoto(id, file.getBytes()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @GetMapping("/{id}")
    public ReportResponse get(@PathVariable Long id) {
        return mapper.toReport(groundReportService.getById(id));
    }
}
