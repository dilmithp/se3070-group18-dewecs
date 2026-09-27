package com.group18.dewecs.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class GroundReportResponse {

    private final Long id;
    private final String reporterName;
    private final String reporterPhone;
    private final String districtName;
    private final Long districtId;
    private final String category;
    private final BigDecimal gpsLat;
    private final BigDecimal gpsLng;
    private final String photoUrl;
    private final String description;
    private final String status;
    private final String actionNote;
    private final String verifiedByName;
    private final LocalDateTime submittedAt;

    public GroundReportResponse(Long id, String reporterName, String reporterPhone, String districtName,
                                 Long districtId, String category, BigDecimal gpsLat, BigDecimal gpsLng,
                                 String photoUrl, String description, String status, String actionNote,
                                 String verifiedByName, LocalDateTime submittedAt) {
        this.id = id;
        this.reporterName = reporterName;
        this.reporterPhone = reporterPhone;
        this.districtName = districtName;
        this.districtId = districtId;
        this.category = category;
        this.gpsLat = gpsLat;
        this.gpsLng = gpsLng;
        this.photoUrl = photoUrl;
        this.description = description;
        this.status = status;
        this.actionNote = actionNote;
        this.verifiedByName = verifiedByName;
        this.submittedAt = submittedAt;
    }

    public Long getId() {
        return id;
    }

    public String getReporterName() {
        return reporterName;
    }

    public String getReporterPhone() {
        return reporterPhone;
    }

    public String getDistrictName() {
        return districtName;
    }

    public Long getDistrictId() {
        return districtId;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getGpsLat() {
        return gpsLat;
    }

    public BigDecimal getGpsLng() {
        return gpsLng;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public String getActionNote() {
        return actionNote;
    }

    public String getVerifiedByName() {
        return verifiedByName;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }
}
