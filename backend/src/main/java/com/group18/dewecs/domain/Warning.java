package com.group18.dewecs.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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

    @NotNull
    private LocalDateTime issuedAt;

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

    public Set<BroadcastChannel> getBroadcastChannels() {
        return broadcastChannels;
    }

    public void setBroadcastChannels(Set<BroadcastChannel> broadcastChannels) {
        this.broadcastChannels = broadcastChannels;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

    public User getIssuedBy() {
        return issuedBy;
    }

    public void setIssuedBy(User issuedBy) {
        this.issuedBy = issuedBy;
    }
}
