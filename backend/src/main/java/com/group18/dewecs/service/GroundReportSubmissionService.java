package com.group18.dewecs.service;

import com.group18.dewecs.domain.GroundReport;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Citizen-side ground reporting (mobile API). The officer review workflow stays in GroundReportService. */
public interface GroundReportSubmissionService {

    /**
     * Stores a new report as PENDING_REVIEW. When capturedAt is given and a report with the same citizen, category
     * and capturedAt already exists, that report is returned unchanged with created=false (safe retries).
     */
    SubmissionResult submit(Long citizenId, Long districtId, String category, String description,
                            BigDecimal gpsLat, BigDecimal gpsLng, LocalDateTime capturedAt);

    /** Stores or replaces the report's photo; only while PENDING_SYNC, PENDING_REVIEW or NEEDS_INFO. */
    GroundReport attachPhoto(Long reportId, byte[] content);

    /** Newest first. A page below 0 becomes 0 and size is clamped to 1..50. */
    Page<GroundReport> listForCitizen(Long citizenId, int page, int size);

    record SubmissionResult(GroundReport report, boolean created) {
    }
}
