package com.group18.dewecs.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** A shelter reporting a shortage of one type of supply. */
public class NeedFormRequest {

    @NotNull(message = "Choose the shelter")
    private Long shelterId;

    @NotBlank(message = "Choose the supply type")
    private String resourceType;

    @NotNull(message = "Enter the quantity needed")
    @Min(value = 1, message = "The quantity must be at least 1")
    private Integer quantity;

    private boolean urgent;
    private String note;

    public Long getShelterId() {
        return shelterId;
    }

    public void setShelterId(Long shelterId) {
        this.shelterId = shelterId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public boolean isUrgent() {
        return urgent;
    }

    public void setUrgent(boolean urgent) {
        this.urgent = urgent;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
