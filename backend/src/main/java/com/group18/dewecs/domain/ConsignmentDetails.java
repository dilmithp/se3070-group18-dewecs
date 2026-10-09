package com.group18.dewecs.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Everything UC-02 records about a consignment beyond the relief_consignments row (which stays unchanged): the
 * incident, handling notes, convoy, the need it serves, the parent of a split batch and the field handover.
 */
@Entity
@Table(name = "consignment_details")
public class ConsignmentDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "consignment_id", unique = true)
    private ReliefConsignment consignment;

    @ManyToOne
    @JoinColumn(name = "hazard_event_id")
    private HazardEvent hazardEvent;

    /** Set on the sub-batches of a split allocation: the first sub-batch is the parent of the others. */
    @ManyToOne
    @JoinColumn(name = "parent_consignment_id")
    private ReliefConsignment parentConsignment;

    @ManyToOne
    @JoinColumn(name = "shelter_need_id")
    private ShelterNeed shelterNeed;

    @Column(length = 1000)
    private String handlingNotes;

    @Column(length = 100)
    private String vehicle;

    private String driverName;

    @Column(length = 50)
    private String driverPhone;

    private LocalDateTime expectedArrival;

    private String receivedBy;

    private Integer receivedQuantity;

    private Integer damagedQuantity;

    @Column(length = 1000)
    private String handoverNote;

    private String handoverPhotoUrl;

    private LocalDateTime handedOverAt;

    public boolean hasHandover() {
        return handedOverAt != null;
    }

    public boolean isDamaged() {
        return damagedQuantity != null && damagedQuantity > 0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ReliefConsignment getConsignment() {
        return consignment;
    }

    public void setConsignment(ReliefConsignment consignment) {
        this.consignment = consignment;
    }

    public HazardEvent getHazardEvent() {
        return hazardEvent;
    }

    public void setHazardEvent(HazardEvent hazardEvent) {
        this.hazardEvent = hazardEvent;
    }

    public ReliefConsignment getParentConsignment() {
        return parentConsignment;
    }

    public void setParentConsignment(ReliefConsignment parentConsignment) {
        this.parentConsignment = parentConsignment;
    }

    public ShelterNeed getShelterNeed() {
        return shelterNeed;
    }

    public void setShelterNeed(ShelterNeed shelterNeed) {
        this.shelterNeed = shelterNeed;
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

    public String getReceivedBy() {
        return receivedBy;
    }

    public void setReceivedBy(String receivedBy) {
        this.receivedBy = receivedBy;
    }

    public Integer getReceivedQuantity() {
        return receivedQuantity;
    }

    public void setReceivedQuantity(Integer receivedQuantity) {
        this.receivedQuantity = receivedQuantity;
    }

    public Integer getDamagedQuantity() {
        return damagedQuantity;
    }

    public void setDamagedQuantity(Integer damagedQuantity) {
        this.damagedQuantity = damagedQuantity;
    }

    public String getHandoverNote() {
        return handoverNote;
    }

    public void setHandoverNote(String handoverNote) {
        this.handoverNote = handoverNote;
    }

    public String getHandoverPhotoUrl() {
        return handoverPhotoUrl;
    }

    public void setHandoverPhotoUrl(String handoverPhotoUrl) {
        this.handoverPhotoUrl = handoverPhotoUrl;
    }

    public LocalDateTime getHandedOverAt() {
        return handedOverAt;
    }

    public void setHandedOverAt(LocalDateTime handedOverAt) {
        this.handedOverAt = handedOverAt;
    }
}
