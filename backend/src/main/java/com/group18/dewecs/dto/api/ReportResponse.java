package com.group18.dewecs.dto.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Contract v1 report object. Never carries reviewer names, phone numbers or other personal data. */
public record ReportResponse(
        Long id,
        Long citizenId,
        Long districtId,
        String districtName,
        String category,
        String description,
        BigDecimal gpsLat,
        BigDecimal gpsLng,
        String photoUrl,
        String status,
        String actionNote,
        LocalDateTime submittedAt) {
}
