package com.group18.dewecs.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Read-only view model for the warning list/detail templates. */
public class WarningResponse {

    private final Long id;
    private final String hazardType;
    private final String districtName;
    private final Long districtId;
    private final String severity;
    private final String status;
    private final String message;
    private final String issuedByName;
    private final LocalDateTime issuedAt;
    private final LocalDateTime expiresAt;
    private final List<String> broadcastChannels;
    private final List<String> affectedDistrictNames;

    public WarningResponse(Long id, String hazardType, String districtName, Long districtId, String severity,
                           String status, String message, String issuedByName, LocalDateTime issuedAt,
                           LocalDateTime expiresAt, List<String> broadcastChannels,
                           List<String> affectedDistrictNames) {
        this.id = id;
        this.hazardType = hazardType;
        this.districtName = districtName;
        this.districtId = districtId;
        this.severity = severity;
        this.status = status;
        this.message = message;
        this.issuedByName = issuedByName;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.broadcastChannels = broadcastChannels;
        this.affectedDistrictNames = affectedDistrictNames;
    }

    public Long getId() {
        return id;
    }

    public String getHazardType() {
        return hazardType;
    }

    public String getDistrictName() {
        return districtName;
    }

    public Long getDistrictId() {
        return districtId;
    }

    public String getSeverity() {
        return severity;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getIssuedByName() {
        return issuedByName;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public List<String> getBroadcastChannels() {
        return broadcastChannels;
    }

    /** Every district the warning covers: the event district, plus the districts of a river basin when one was chosen. */
    public List<String> getAffectedDistrictNames() {
        return affectedDistrictNames;
    }
}
