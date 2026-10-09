package com.group18.dewecs.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** The point on the map where a hazard event happens (the hazard_events table has no coordinates). */
@Entity
@Table(name = "hazard_event_locations")
public class HazardEventLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @OneToOne
    @JoinColumn(name = "hazard_event_id", unique = true)
    private HazardEvent hazardEvent;

    @NotNull
    @Column(precision = 9, scale = 6)
    private BigDecimal lat;

    @NotNull
    @Column(precision = 9, scale = 6)
    private BigDecimal lng;

    public Long getId() {
        return id;
    }

    public HazardEvent getHazardEvent() {
        return hazardEvent;
    }

    public void setHazardEvent(HazardEvent hazardEvent) {
        this.hazardEvent = hazardEvent;
    }

    public BigDecimal getLat() {
        return lat;
    }

    public void setLat(BigDecimal lat) {
        this.lat = lat;
    }

    public BigDecimal getLng() {
        return lng;
    }

    public void setLng(BigDecimal lng) {
        this.lng = lng;
    }
}
