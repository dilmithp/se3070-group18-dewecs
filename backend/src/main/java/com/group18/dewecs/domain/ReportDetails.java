package com.group18.dewecs.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * The parameters and status of a post-event report that the shared post_event_reports table has no room for: the
 * analysis window, an optional donor filter, the provisional flag with its reason and the approval.
 */
@Entity
@Table(name = "report_details")
public class ReportDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "report_id", unique = true)
    private PostEventReport report;

    private LocalDateTime windowFrom;

    private LocalDateTime windowTo;

    /** When set, the KPIs count only what this organisation contributed (audit statement for one donor). */
    @ManyToOne
    @JoinColumn(name = "donor_organization_id")
    private Organization donor;

    @NotNull
    private Boolean provisional = false;

    @Column(length = 1000)
    private String provisionalReason;

    @Column(length = 1000)
    private String dataGaps;

    private String approvedBy;

    private LocalDateTime approvedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PostEventReport getReport() {
        return report;
    }

    public void setReport(PostEventReport report) {
        this.report = report;
    }

    public LocalDateTime getWindowFrom() {
        return windowFrom;
    }

    public void setWindowFrom(LocalDateTime windowFrom) {
        this.windowFrom = windowFrom;
    }

    public LocalDateTime getWindowTo() {
        return windowTo;
    }

    public void setWindowTo(LocalDateTime windowTo) {
        this.windowTo = windowTo;
    }

    public Organization getDonor() {
        return donor;
    }

    public void setDonor(Organization donor) {
        this.donor = donor;
    }

    public Boolean getProvisional() {
        return provisional;
    }

    public void setProvisional(Boolean provisional) {
        this.provisional = provisional;
    }

    public String getProvisionalReason() {
        return provisionalReason;
    }

    public void setProvisionalReason(String provisionalReason) {
        this.provisionalReason = provisionalReason;
    }

    public String getDataGaps() {
        return dataGaps;
    }

    public void setDataGaps(String dataGaps) {
        this.dataGaps = dataGaps;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }
}
