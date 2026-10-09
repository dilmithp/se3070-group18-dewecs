package com.group18.dewecs.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/** The dispatch order of the relief command center: what goes where, with handling notes and the convoy. */
public class DispatchFormRequest {

    private Long hazardEventId;
    private Long districtId;

    @NotNull(message = "Choose the supply to send")
    private Long resourceId;

    @NotNull(message = "Choose the destination shelter")
    private Long shelterId;

    @NotNull(message = "Enter a quantity")
    @Min(value = 1, message = "The quantity must be at least 1")
    private Integer quantity;

    private Long needId;
    private String handlingNotes;
    private String vehicle;
    private String driverName;
    private String driverPhone;
    private LocalDateTime expectedArrival;

    /** The stock of the chosen supply when the form was shown (detects a concurrent dispatch). */
    private Integer expectedStock;

    private boolean acceptSplit;
    private String officerName;

    public Long getHazardEventId() {
        return hazardEventId;
    }

    public void setHazardEventId(Long hazardEventId) {
        this.hazardEventId = hazardEventId;
    }

    public Long getDistrictId() {
        return districtId;
    }

    public void setDistrictId(Long districtId) {
        this.districtId = districtId;
    }

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

    public Long getNeedId() {
        return needId;
    }

    public void setNeedId(Long needId) {
        this.needId = needId;
    }

    public String getHandlingNotes() {
        return handlingNotes;
    }

    public void setHandlingNotes(String handlingNotes) {
        this.handlingNotes = handlingNotes;
    }

    public String getVehicle() {
        return vehicle;
    }

    public void setVehicle(String vehicle) {
        this.vehicle = vehicle;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public String getDriverPhone() {
        return driverPhone;
    }

    public void setDriverPhone(String driverPhone) {
        this.driverPhone = driverPhone;
    }

    public LocalDateTime getExpectedArrival() {
        return expectedArrival;
    }

    public void setExpectedArrival(LocalDateTime expectedArrival) {
        this.expectedArrival = expectedArrival;
    }

    public Integer getExpectedStock() {
        return expectedStock;
    }

    public void setExpectedStock(Integer expectedStock) {
        this.expectedStock = expectedStock;
    }

    public boolean isAcceptSplit() {
        return acceptSplit;
    }

    public void setAcceptSplit(boolean acceptSplit) {
        this.acceptSplit = acceptSplit;
    }

    public String getOfficerName() {
        return officerName;
    }

    public void setOfficerName(String officerName) {
        this.officerName = officerName;
    }
}
