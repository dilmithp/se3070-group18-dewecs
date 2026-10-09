package com.group18.dewecs.dto;

import java.time.LocalDateTime;

public class HazardEventResponse {

    private final Long id;
    private final String hazardType;
    private final String severity;
    private final String status;
    private final String districtName;
    private final Long districtId;
    private final LocalDateTime occurredAt;
    private final int verifiedReports;
    private final int liveWarnings;

    public HazardEventResponse(Long id, String hazardType, String severity, String status, String districtName,
                                Long districtId, LocalDateTime occurredAt, int verifiedReports, int liveWarnings) {
        this.id = id;
        this.hazardType = hazardType;
        this.severity = severity;
        this.status = status;
        this.districtName = districtName;
        this.districtId = districtId;
        this.occurredAt = occurredAt;
        this.verifiedReports = verifiedReports;
        this.liveWarnings = liveWarnings;
    }

    public Long getId() {
        return id;
    }

    public String getHazardType() {
        return hazardType;
    }

    public String getSeverity() {
        return severity;
    }

    public String getStatus() {
        return status;
    }

    public String getDistrictName() {
        return districtName;
    }

    public Long getDistrictId() {
        return districtId;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    /** Verified or actioned ground reports in the district around the event (0 when not computed). */
    public int getVerifiedReports() {
        return verifiedReports;
    }

    /** Warnings of the event that are live now (0 when not computed). */
    public int getLiveWarnings() {
        return liveWarnings;
    }
}
