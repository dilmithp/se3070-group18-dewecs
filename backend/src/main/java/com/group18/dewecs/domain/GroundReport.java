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
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ground_reports")
public class GroundReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "reported_by_citizen_id")
    private Citizen reportedBy;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "district_id")
    private District district;

    @NotNull
    @Enumerated(EnumType.STRING)
    private HazardType category;

    private String photoUrl;

    @NotNull
    private BigDecimal gpsLat;

    @NotNull
    private BigDecimal gpsLng;

    @Column(length = 2000)
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    private GroundReportStatus status;

    @ManyToOne
    @JoinColumn(name = "verified_by_user_id")
    private User verifiedBy;

    @Column(length = 2000)
    private String actionNote;

    @NotNull
    private LocalDateTime submittedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Citizen getReportedBy() {
        return reportedBy;
    }

    public void setReportedBy(Citizen reportedBy) {
        this.reportedBy = reportedBy;
    }

    public District getDistrict() {
        return district;
    }

    public void setDistrict(District district) {
        this.district = district;
    }

    public HazardType getCategory() {
        return category;
    }

    public void setCategory(HazardType category) {
        this.category = category;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
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

    public GroundReportStatus getStatus() {
        return status;
    }

    public void setStatus(GroundReportStatus status) {
        this.status = status;
    }

    public User getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(User verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public String getActionNote() {
        return actionNote;
    }

    public void setActionNote(String actionNote) {
        this.actionNote = actionNote;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
}
