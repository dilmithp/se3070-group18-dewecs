package com.group18.dewecs.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Links to {@link Warning}s and {@link Shelter}s are the corrections from our
 * critique report: the original design had no way to derive "citizens reached" /
 * alert-timeline metrics or shelter-occupancy-over-time metrics for a report.
 */
@Entity
@Table(name = "post_event_reports")
public class PostEventReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "district_id")
    private District district;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "hazard_event_id")
    private HazardEvent hazardEvent;

    @NotNull
    private LocalDateTime generatedAt;

    @ManyToMany
    @JoinTable(
            name = "post_event_report_warnings",
            joinColumns = @JoinColumn(name = "report_id"),
            inverseJoinColumns = @JoinColumn(name = "warning_id")
    )
    private Set<Warning> relatedWarnings = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "post_event_report_shelters",
            joinColumns = @JoinColumn(name = "report_id"),
            inverseJoinColumns = @JoinColumn(name = "shelter_id")
    )
    private Set<Shelter> relatedShelters = new HashSet<>();

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

    public HazardEvent getHazardEvent() {
        return hazardEvent;
    }

    public void setHazardEvent(HazardEvent hazardEvent) {
        this.hazardEvent = hazardEvent;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public Set<Warning> getRelatedWarnings() {
        return relatedWarnings;
    }

    public void setRelatedWarnings(Set<Warning> relatedWarnings) {
        this.relatedWarnings = relatedWarnings;
    }

    public Set<Shelter> getRelatedShelters() {
        return relatedShelters;
    }

    public void setRelatedShelters(Set<Shelter> relatedShelters) {
        this.relatedShelters = relatedShelters;
    }
}
