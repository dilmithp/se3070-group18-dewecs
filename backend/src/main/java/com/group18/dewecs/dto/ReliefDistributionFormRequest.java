package com.group18.dewecs.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class ReliefDistributionFormRequest {

    @NotNull(message = "Select a relief supply")
    private Long resourceId;

    @NotNull(message = "Select a destination shelter")
    private Long shelterId;

    @NotNull(message = "Enter a quantity")
    @Positive(message = "Quantity must be positive")
    private Integer quantity;

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public Long getShelterId() {
        return shelterId;
    }

    public void setShelterId(Long shelterId) {
        this.shelterId = shelterId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
