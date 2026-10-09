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

import java.time.LocalDateTime;

/** One line of the district coordination audit trail: who did what, for which organization, and when. */
@Entity
@Table(name = "coordination_events")
public class CoordinationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "district_id")
    private District district;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type")
    private CoordinationEventType type;

    private String subject;

    @Column(length = 500)
    private String detail;

    private String recordedBy;

    @ManyToOne
    @JoinColumn(name = "organization_id")
    private Organization organization;

    /** When the action happened on the device, if it was queued while offline. */
    private LocalDateTime clientTime;

    /** True for a conflicting update that needs a manual look. */
    @Column(nullable = false)
    private boolean needsReview;

    @NotNull
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public District getDistrict() {
        return district;
    }

    public void setDistrict(District district) {
        this.district = district;
    }

    public CoordinationEventType getType() {
        return type;
    }

    public void setType(CoordinationEventType type) {
        this.type = type;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getRecordedBy() {
        return recordedBy;
    }

    public void setRecordedBy(String recordedBy) {
        this.recordedBy = recordedBy;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public LocalDateTime getClientTime() {
        return clientTime;
    }

    public void setClientTime(LocalDateTime clientTime) {
        this.clientTime = clientTime;
    }

    public boolean isNeedsReview() {
        return needsReview;
    }

    public void setNeedsReview(boolean needsReview) {
        this.needsReview = needsReview;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
