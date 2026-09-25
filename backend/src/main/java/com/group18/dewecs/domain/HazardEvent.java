package com.group18.dewecs.domain;

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

@Entity
@Table(name = "hazard_events")
public class HazardEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    private HazardType hazardType;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Severity severityLevel;

    @NotNull
    @Enumerated(EnumType.STRING)
    private HazardEventStatus status;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "district_id")
    private District district;

    @NotNull
    private LocalDateTime occurredAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public HazardType getHazardType() {
        return hazardType;
    }

    public void setHazardType(HazardType hazardType) {
        this.hazardType = hazardType;
    }

    public Severity getSeverityLevel() {
        return severityLevel;
    }

    public void setSeverityLevel(Severity severityLevel) {
        this.severityLevel = severityLevel;
    }

    public HazardEventStatus getStatus() {
        return status;
    }

    public void setStatus(HazardEventStatus status) {
        this.status = status;
    }

    public District getDistrict() {
        return district;
    }

    public void setDistrict(District district) {
        this.district = district;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }
}
