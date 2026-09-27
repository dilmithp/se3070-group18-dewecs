package com.group18.dewecs.dto;

public class ReliefSupplyResponse {

    private final Long id;
    private final String name;
    private final String type;
    private final String unit;
    private final Integer quantity;
    private final String districtName;
    private final Long districtId;
    private final String organizationName;
    private final boolean lowStock;
    private final boolean outOfStock;

    public ReliefSupplyResponse(Long id, String name, String type, String unit, Integer quantity,
                                 String districtName, Long districtId, String organizationName,
                                 boolean lowStock, boolean outOfStock) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.unit = unit;
        this.quantity = quantity;
        this.districtName = districtName;
        this.districtId = districtId;
        this.organizationName = organizationName;
        this.lowStock = lowStock;
        this.outOfStock = outOfStock;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getUnit() {
        return unit;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public String getDistrictName() {
        return districtName;
    }

    public Long getDistrictId() {
        return districtId;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public boolean isLowStock() {
        return lowStock;
    }

    public boolean isOutOfStock() {
        return outOfStock;
    }
}
