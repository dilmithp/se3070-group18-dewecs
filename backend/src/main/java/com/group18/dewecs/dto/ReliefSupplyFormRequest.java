package com.group18.dewecs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public class ReliefSupplyFormRequest {

    @NotNull(message = "Select a district")
    private Long districtId;

    @NotNull(message = "Select an owning organization")
    private Long organizationId;

    @NotBlank(message = "Enter a name")
    private String name;

    @NotBlank(message = "Select a category")
    private String type;

    @NotBlank(message = "Enter a unit (e.g. kg, boxes)")
    private String unit;

    @NotNull(message = "Enter a quantity")
    @PositiveOrZero(message = "Quantity cannot be negative")
    private Integer quantity;

    public Long getDistrictId() {
        return districtId;
    }

    public void setDistrictId(Long districtId) {
        this.districtId = districtId;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
