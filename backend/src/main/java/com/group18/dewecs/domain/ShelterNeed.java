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

/**
 * A request for supplies from an evacuation shelter (the shelter demand feed of UC-02). Allocations raise
 * {@code quantityAllocated}; when it reaches {@code quantityNeeded} the need is MET.
 */
@Entity
@Table(name = "shelter_needs")
public class ShelterNeed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "shelter_id")
    private Shelter shelter;

    @NotNull
    @Enumerated(EnumType.STRING)
    private ResourceType resourceType;

    @NotNull
    private Integer quantityNeeded;

    @NotNull
    private Integer quantityAllocated = 0;

    @NotNull
    private Boolean urgent = false;

    @NotNull
    @Enumerated(EnumType.STRING)
    private NeedStatus status = NeedStatus.OPEN;

    @Column(length = 500)
    private String note;

    @NotNull
    private LocalDateTime requestedAt;

    public int remaining() {
        return Math.max(0, quantityNeeded - quantityAllocated);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Shelter getShelter() {
        return shelter;
    }

    public void setShelter(Shelter shelter) {
        this.shelter = shelter;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public Integer getQuantityNeeded() {
        return quantityNeeded;
    }

    public void setQuantityNeeded(Integer quantityNeeded) {
        this.quantityNeeded = quantityNeeded;
    }

    public Integer getQuantityAllocated() {
        return quantityAllocated;
    }

    public void setQuantityAllocated(Integer quantityAllocated) {
        this.quantityAllocated = quantityAllocated;
    }

    public Boolean getUrgent() {
        return urgent;
    }

    public void setUrgent(Boolean urgent) {
        this.urgent = urgent;
    }

    public NeedStatus getStatus() {
        return status;
    }

    public void setStatus(NeedStatus status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }
}
