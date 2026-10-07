package com.group18.dewecs.dto.api;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** photoUrl cannot be set here; unknown JSON properties are ignored. */
public record CreateGroundReportRequest(
        @NotNull(message = "citizenId is required") Long citizenId,
        @NotNull(message = "districtId is required") Long districtId,
        @NotBlank(message = "Select a category") String category,
        @NotBlank(message = "Describe what you see") String description,
        @NotNull(message = "gpsLat is required")
        @DecimalMin(value = "-90", message = "gpsLat must be between -90 and 90")
        @DecimalMax(value = "90", message = "gpsLat must be between -90 and 90") BigDecimal gpsLat,
        @NotNull(message = "gpsLng is required")
        @DecimalMin(value = "-180", message = "gpsLng must be between -180 and 180")
        @DecimalMax(value = "180", message = "gpsLng must be between -180 and 180") BigDecimal gpsLng,
        LocalDateTime capturedAt) {
}
