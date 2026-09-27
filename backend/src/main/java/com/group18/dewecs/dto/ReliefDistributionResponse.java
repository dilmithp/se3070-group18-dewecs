package com.group18.dewecs.dto;

import java.time.LocalDateTime;

public class ReliefDistributionResponse {

    private final Long id;
    private final String resourceName;
    private final String resourceUnit;
    private final Integer quantity;
    private final String shelterName;
    private final Long shelterId;
    private final String status;
    private final String organizationName;
    private final LocalDateTime dispatchedAt;
    private final LocalDateTime deliveredAt;

    public ReliefDistributionResponse(Long id, String resourceName, String resourceUnit, Integer quantity,
                                       String shelterName, Long shelterId, String status, String organizationName,
                                       LocalDateTime dispatchedAt, LocalDateTime deliveredAt) {
        this.id = id;
        this.resourceName = resourceName;
        this.resourceUnit = resourceUnit;
        this.quantity = quantity;
        this.shelterName = shelterName;
        this.shelterId = shelterId;
        this.status = status;
        this.organizationName = organizationName;
        this.dispatchedAt = dispatchedAt;
        this.deliveredAt = deliveredAt;
    }

    public Long getId() {
        return id;
    }

    public String getResourceName() {
        return resourceName;
    }

    public String getResourceUnit() {
        return resourceUnit;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public String getShelterName() {
        return shelterName;
    }

    public Long getShelterId() {
        return shelterId;
    }

    public String getStatus() {
        return status;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public LocalDateTime getDispatchedAt() {
        return dispatchedAt;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }
}
