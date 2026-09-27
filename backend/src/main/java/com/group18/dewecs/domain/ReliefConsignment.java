package com.group18.dewecs.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Destination is {@link Shelter} (Phase 4 requirement) rather than the {@link District} this
 * originally pointed at in Phase 1 — a shelter's own district still gives the region if needed.
 */
@Entity
@Table(name = "relief_consignments")
public class ReliefConsignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "shelter_id")
    private Shelter shelter;

    @NotNull
    @Enumerated(EnumType.STRING)
    private ConsignmentStatus status;

    @NotNull
    private LocalDateTime dispatchedAt;

    private LocalDateTime deliveredAt;

    /** Uses the existing FK on {@link ConsignmentItem}; no schema change. Eager, matching the
     * existing precedent for small owned collections (see Warning.broadcastChannels). */
    @OneToMany(mappedBy = "consignment", fetch = FetchType.EAGER)
    private List<ConsignmentItem> items = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public Shelter getShelter() {
        return shelter;
    }

    public void setShelter(Shelter shelter) {
        this.shelter = shelter;
    }

    public ConsignmentStatus getStatus() {
        return status;
    }

    public void setStatus(ConsignmentStatus status) {
        this.status = status;
    }

    public LocalDateTime getDispatchedAt() {
        return dispatchedAt;
    }

    public void setDispatchedAt(LocalDateTime dispatchedAt) {
        this.dispatchedAt = dispatchedAt;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public List<ConsignmentItem> getItems() {
        return items;
    }

    public void setItems(List<ConsignmentItem> items) {
        this.items = items;
    }
}
