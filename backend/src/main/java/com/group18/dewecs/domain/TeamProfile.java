package com.group18.dewecs.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Coordination data of a rescue team that the shared rescue_teams table does not hold. */
@Entity
@Table(name = "rescue_team_profiles")
public class TeamProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @OneToOne
    @JoinColumn(name = "team_id", unique = true)
    private RescueTeam team;

    @NotNull
    @Enumerated(EnumType.STRING)
    private TeamCapability capability = TeamCapability.GENERAL;

    @Column(precision = 9, scale = 6)
    private BigDecimal baseLat;

    @Column(precision = 9, scale = 6)
    private BigDecimal baseLng;

    @Enumerated(EnumType.STRING)
    private TeamStage stage;

    private LocalDateTime statusUpdatedAt;

    @Column(length = 500)
    private String supportNote;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RescueTeam getTeam() {
        return team;
    }

    public void setTeam(RescueTeam team) {
        this.team = team;
    }

    public TeamCapability getCapability() {
        return capability;
    }

    public void setCapability(TeamCapability capability) {
        this.capability = capability;
    }

    public BigDecimal getBaseLat() {
        return baseLat;
    }

    public void setBaseLat(BigDecimal baseLat) {
        this.baseLat = baseLat;
    }

    public BigDecimal getBaseLng() {
        return baseLng;
    }

    public void setBaseLng(BigDecimal baseLng) {
        this.baseLng = baseLng;
    }

    public TeamStage getStage() {
        return stage;
    }

    public void setStage(TeamStage stage) {
        this.stage = stage;
    }

    public LocalDateTime getStatusUpdatedAt() {
        return statusUpdatedAt;
    }

    public void setStatusUpdatedAt(LocalDateTime statusUpdatedAt) {
        this.statusUpdatedAt = statusUpdatedAt;
    }

    public String getSupportNote() {
        return supportNote;
    }

    public void setSupportNote(String supportNote) {
        this.supportNote = supportNote;
    }
}
