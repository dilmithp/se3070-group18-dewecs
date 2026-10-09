package com.group18.dewecs.domain;

import jakarta.persistence.Column;
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

/**
 * One row per delivery attempt of a warning on a channel: what was tried, whether it worked, and (for a fallback)
 * which channel it replaced. This is the delivery log of UC-01.
 */
@Entity
@Table(name = "alert_delivery_logs")
public class AlertDeliveryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "warning_id")
    private Warning warning;

    @NotNull
    @Enumerated(EnumType.STRING)
    private BroadcastChannel channel;

    @NotNull
    private Integer attempt;

    @NotNull
    @Enumerated(EnumType.STRING)
    private DeliveryOutcome outcome;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type")
    private DeliveryTrigger trigger;

    /** The channel that failed and that this attempt replaced; null for a normal attempt. */
    @Enumerated(EnumType.STRING)
    private BroadcastChannel fallbackFor;

    /** Registered citizens addressed by this attempt; null for mass media (radio, TV, siren). */
    private Integer recipients;

    @Column(length = 500)
    private String detail;

    @NotNull
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Warning getWarning() {
        return warning;
    }

    public void setWarning(Warning warning) {
        this.warning = warning;
    }

    public BroadcastChannel getChannel() {
        return channel;
    }

    public void setChannel(BroadcastChannel channel) {
        this.channel = channel;
    }

    public Integer getAttempt() {
        return attempt;
    }

    public void setAttempt(Integer attempt) {
        this.attempt = attempt;
    }

    public DeliveryOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(DeliveryOutcome outcome) {
        this.outcome = outcome;
    }

    public DeliveryTrigger getTrigger() {
        return trigger;
    }

    public void setTrigger(DeliveryTrigger trigger) {
        this.trigger = trigger;
    }

    public BroadcastChannel getFallbackFor() {
        return fallbackFor;
    }

    public void setFallbackFor(BroadcastChannel fallbackFor) {
        this.fallbackFor = fallbackFor;
    }

    public Integer getRecipients() {
        return recipients;
    }

    public void setRecipients(Integer recipients) {
        this.recipients = recipients;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
