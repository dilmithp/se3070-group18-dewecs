package com.group18.dewecs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/** Registers a hazard event. A blank start time means now. */
public class HazardEventFormRequest {

    @NotBlank(message = "Select a hazard type")
    private String hazardType;

    @NotBlank(message = "Select a severity level")
    private String severity;

    @NotNull(message = "Select the affected district")
    private Long districtId;

    private LocalDateTime occurredAt;

    private java.math.BigDecimal gpsLat;

    private java.math.BigDecimal gpsLng;

    public java.math.BigDecimal getGpsLat() {
        return gpsLat;
    }

    public void setGpsLat(java.math.BigDecimal gpsLat) {
        this.gpsLat = gpsLat;
    }

    public java.math.BigDecimal getGpsLng() {
        return gpsLng;
    }

    public void setGpsLng(java.math.BigDecimal gpsLng) {
        this.gpsLng = gpsLng;
    }

    public String getHazardType() {
        return hazardType;
    }

    public void setHazardType(String hazardType) {
        this.hazardType = hazardType;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public Long getDistrictId() {
        return districtId;
    }

    public void setDistrictId(Long districtId) {
        this.districtId = districtId;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }
}
