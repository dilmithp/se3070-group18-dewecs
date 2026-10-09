package com.group18.dewecs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.HashSet;
import java.util.Set;

/** The one-screen form "Add New Hazard Event": event, area and warning in one go. */
public class IssueWarningFormRequest {

    @NotBlank(message = "Select a hazard type")
    private String hazardType;

    @NotBlank(message = "Select a severity level")
    private String severity;

    /** DISTRICT or BASIN: which of the two area choices is used. */
    private String areaMode = "DISTRICT";

    private Long districtId;

    private Long riverBasinId;

    private String title;

    @NotBlank(message = "Enter the warning message")
    private String message;

    private Set<String> broadcastChannels = new HashSet<>(Set.of("SMS", "APP_PUSH", "SIREN"));

    @NotNull(message = "Select the officer issuing this warning")
    private Long issuedByUserId;

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

    public String getAreaMode() {
        return areaMode;
    }

    public void setAreaMode(String areaMode) {
        this.areaMode = areaMode;
    }

    public Long getDistrictId() {
        return districtId;
    }

    public void setDistrictId(Long districtId) {
        this.districtId = districtId;
    }

    public Long getRiverBasinId() {
        return riverBasinId;
    }

    public void setRiverBasinId(Long riverBasinId) {
        this.riverBasinId = riverBasinId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Set<String> getBroadcastChannels() {
        return broadcastChannels;
    }

    public void setBroadcastChannels(Set<String> broadcastChannels) {
        this.broadcastChannels = broadcastChannels;
    }

    public Long getIssuedByUserId() {
        return issuedByUserId;
    }

    public void setIssuedByUserId(Long issuedByUserId) {
        this.issuedByUserId = issuedByUserId;
    }
}
