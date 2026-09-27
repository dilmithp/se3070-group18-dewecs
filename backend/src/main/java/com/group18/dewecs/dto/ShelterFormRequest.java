package com.group18.dewecs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class ShelterFormRequest {

    @NotNull(message = "Select a district")
    private Long districtId;

    @NotNull(message = "Select an owning organization")
    private Long organizationId;

    @NotBlank(message = "Enter a name")
    private String name;

    @NotNull(message = "Enter a capacity")
    @Positive(message = "Capacity must be positive")
    private Integer capacity;

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

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }
}
