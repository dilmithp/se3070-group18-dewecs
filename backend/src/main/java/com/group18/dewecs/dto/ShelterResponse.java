package com.group18.dewecs.dto;

public class ShelterResponse {

    private final Long id;
    private final String name;
    private final String districtName;
    private final Long districtId;
    private final Integer capacity;
    private final Integer currentOccupancy;
    private final String status;
    private final String organizationName;

    public ShelterResponse(Long id, String name, String districtName, Long districtId, Integer capacity,
                            Integer currentOccupancy, String status, String organizationName) {
        this.id = id;
        this.name = name;
        this.districtName = districtName;
        this.districtId = districtId;
        this.capacity = capacity;
        this.currentOccupancy = currentOccupancy;
        this.status = status;
        this.organizationName = organizationName;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDistrictName() {
        return districtName;
    }

    public Long getDistrictId() {
        return districtId;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public Integer getCurrentOccupancy() {
        return currentOccupancy;
    }

    public String getStatus() {
        return status;
    }

    public String getOrganizationName() {
        return organizationName;
    }
}
