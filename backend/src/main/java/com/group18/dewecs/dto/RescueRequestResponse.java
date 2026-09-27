package com.group18.dewecs.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RescueRequestResponse {

    private final Long id;
    private final String requesterName;
    private final String requesterPhone;
    private final String districtName;
    private final Long districtId;
    private final BigDecimal gpsLat;
    private final BigDecimal gpsLng;
    private final String description;
    private final String priority;
    private final String status;
    private final String assignedTeamName;
    private final LocalDateTime submittedAt;
    private final LocalDateTime assignedAt;
    private final LocalDateTime completedAt;

    public RescueRequestResponse(Long id, String requesterName, String requesterPhone, String districtName,
                                  Long districtId, BigDecimal gpsLat, BigDecimal gpsLng, String description,
                                  String priority, String status, String assignedTeamName,
                                  LocalDateTime submittedAt, LocalDateTime assignedAt, LocalDateTime completedAt) {
        this.id = id;
        this.requesterName = requesterName;
        this.requesterPhone = requesterPhone;
        this.districtName = districtName;
        this.districtId = districtId;
        this.gpsLat = gpsLat;
        this.gpsLng = gpsLng;
        this.description = description;
        this.priority = priority;
        this.status = status;
        this.assignedTeamName = assignedTeamName;
        this.submittedAt = submittedAt;
        this.assignedAt = assignedAt;
        this.completedAt = completedAt;
    }

    public Long getId() {
        return id;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public String getRequesterPhone() {
        return requesterPhone;
    }

    public String getDistrictName() {
        return districtName;
    }

    public Long getDistrictId() {
        return districtId;
    }

    public BigDecimal getGpsLat() {
        return gpsLat;
    }

    public BigDecimal getGpsLng() {
        return gpsLng;
    }

    public String getDescription() {
        return description;
    }

    public String getPriority() {
        return priority;
    }

    public String getStatus() {
        return status;
    }

    public String getAssignedTeamName() {
        return assignedTeamName;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
}
