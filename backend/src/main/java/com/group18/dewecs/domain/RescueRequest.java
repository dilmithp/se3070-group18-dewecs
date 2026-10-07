package com.group18.dewecs.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Priority reuses {@link Severity} (LOW/MODERATE/HIGH/CRITICAL) rather than a near-duplicate
 * enum, since the scale is the same one already used for hazard/warning severity.
 */
@Entity
@Table(name = "rescue_requests")
public class RescueRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String requesterName;

    @NotBlank
    private String requesterPhone;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "district_id")
    private District district;

    @Column(precision = 10, scale = 7)
    private BigDecimal gpsLat;

    @Column(precision = 10, scale = 7)
    private BigDecimal gpsLng;

    @NotBlank
    @Column(length = 2000)
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Severity priority;

    @NotNull
    @Enumerated(EnumType.STRING)
    private RescueRequestStatus status;

    @ManyToOne
    @JoinColumn(name = "assigned_team_id")
    private RescueTeam assignedTeam;

    @NotNull
    private LocalDateTime submittedAt;

    private LocalDateTime assignedAt;

    private LocalDateTime completedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public void setRequesterName(String requesterName) {
        this.requesterName = requesterName;
    }

    public String getRequesterPhone() {
        return requesterPhone;
    }

    public void setRequesterPhone(String requesterPhone) {
        this.requesterPhone = requesterPhone;
    }

    public District getDistrict() {
        return district;
    }

    public void setDistrict(District district) {
        this.district = district;
    }

    public BigDecimal getGpsLat() {
        return gpsLat;
    }

    public void setGpsLat(BigDecimal gpsLat) {
        this.gpsLat = gpsLat;
    }

    public BigDecimal getGpsLng() {
        return gpsLng;
    }

    public void setGpsLng(BigDecimal gpsLng) {
        this.gpsLng = gpsLng;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Severity getPriority() {
        return priority;
    }

    public void setPriority(Severity priority) {
        this.priority = priority;
    }

    public RescueRequestStatus getStatus() {
        return status;
    }

    public void setStatus(RescueRequestStatus status) {
        this.status = status;
    }

    public RescueTeam getAssignedTeam() {
        return assignedTeam;
    }

    public void setAssignedTeam(RescueTeam assignedTeam) {
        this.assignedTeam = assignedTeam;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
