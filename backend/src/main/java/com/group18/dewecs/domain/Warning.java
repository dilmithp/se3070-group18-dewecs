package com.group18.dewecs.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "warnings")
public class Warning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "hazard_event_id")
    private HazardEvent hazardEvent;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Severity severity;

    @NotNull
    @Enumerated(EnumType.STRING)
    private WarningStatus status;

    @ElementCollection(fetch = jakarta.persistence.FetchType.EAGER)
    @CollectionTable(name = "warning_broadcast_channels", joinColumns = @JoinColumn(name = "warning_id"))
    @Enumerated(EnumType.STRING)
    private Set<BroadcastChannel> broadcastChannels = new HashSet<>();

    @Column(length = 2000)
    private String message;

    /** Null while {@link WarningStatus#DRAFT}; stamped with the actual time at publish. */
    private LocalDateTime issuedAt;

    private LocalDateTime expiresAt;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "issued_by_user_id")
    private User issuedBy;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public HazardEvent getHazardEvent() {
        return hazardEvent;
    }

    public void setHazardEvent(HazardEvent hazardEvent) {
        this.hazardEvent = hazardEvent;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public WarningStatus getStatus() {
        return status;
    }

    public void setStatus(WarningStatus status) {
        this.status = status;
    }

    /**
     * Extra districts this warning covers (a river-basin warning). Empty means just the district of the hazard event;
     * when a basin was chosen it holds the event district and every district of the basin. Stored in the new join
     * table warning_districts, so the warnings table itself is unchanged.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "warning_districts",
            joinColumns = @JoinColumn(name = "warning_id"),
            inverseJoinColumns = @JoinColumn(name = "district_id")
    )
    private Set<District> affectedDistricts = new HashSet<>();

    public Set<District> getAffectedDistricts() {
        return affectedDistricts;
    }

    public void setAffectedDistricts(Set<District> affectedDistricts) {
        this.affectedDistricts = affectedDistricts;
    }

    /** Every district this warning covers: the chosen set, or just the district of the hazard event. */
    public Set<District> effectiveDistricts() {
        if (affectedDistricts != null && !affectedDistricts.isEmpty()) {
            return affectedDistricts;
        }
        Set<District> one = new HashSet<>();
        if (hazardEvent != null && hazardEvent.getDistrict() != null) {
            one.add(hazardEvent.getDistrict());
        }
        return one;
    }

    public Set<BroadcastChannel> getBroadcastChannels() {
        return broadcastChannels;
    }

    public void setBroadcastChannels(Set<BroadcastChannel> broadcastChannels) {
        this.broadcastChannels = broadcastChannels;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public User getIssuedBy() {
        return issuedBy;
    }

    public void setIssuedBy(User issuedBy) {
        this.issuedBy = issuedBy;
    }
}
