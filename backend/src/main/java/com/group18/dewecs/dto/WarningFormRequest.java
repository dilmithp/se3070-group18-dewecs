package com.group18.dewecs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/** Backs both the create and edit forms for a draft warning. */
public class WarningFormRequest {

    @NotNull(message = "Select a hazard event")
    private Long hazardEventId;

    @NotNull(message = "Select the officer issuing this warning")
    private Long issuedByUserId;

    @NotBlank(message = "Select a severity level")
    private String severity;

    private String message;

    private LocalDateTime expiresAt;

    private Set<String> broadcastChannels = new HashSet<>();

    /** Optional: warn every district of this river basin as well. On an edit, blank keeps the current districts. */
    private Long riverBasinId;

    public Long getHazardEventId() {
        return hazardEventId;
    }

    public void setHazardEventId(Long hazardEventId) {
        this.hazardEventId = hazardEventId;
    }

    public Long getIssuedByUserId() {
        return issuedByUserId;
    }

    public void setIssuedByUserId(Long issuedByUserId) {
        this.issuedByUserId = issuedByUserId;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Set<String> getBroadcastChannels() {
        return broadcastChannels;
    }

    public void setBroadcastChannels(Set<String> broadcastChannels) {
        this.broadcastChannels = broadcastChannels;
    }

    public Long getRiverBasinId() {
        return riverBasinId;
    }

    public void setRiverBasinId(Long riverBasinId) {
        this.riverBasinId = riverBasinId;
    }
}
