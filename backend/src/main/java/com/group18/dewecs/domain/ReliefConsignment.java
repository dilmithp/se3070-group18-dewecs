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
    @JoinColumn(name = "district_id")
    private District district;

    @NotNull
    @Enumerated(EnumType.STRING)
    private ConsignmentStatus status;

    @NotNull
    private LocalDateTime dispatchedAt;

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

    public District getDistrict() {
        return district;
    }

    public void setDistrict(District district) {
        this.district = district;
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
}
