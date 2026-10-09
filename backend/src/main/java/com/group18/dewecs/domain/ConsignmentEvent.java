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

/** One line of the logistics audit log of a consignment: who did what, and when. */
@Entity
@Table(name = "consignment_events")
public class ConsignmentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "consignment_id")
    private ReliefConsignment consignment;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type")
    private ConsignmentEventType type;

    @Column(length = 500)
    private String detail;

    private String recordedBy;

    @NotNull
    private LocalDateTime createdAt;

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

    public ConsignmentEventType getType() {
        return type;
    }

    public void setType(ConsignmentEventType type) {
        this.type = type;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getRecordedBy() {
        return recordedBy;
    }

    public void setRecordedBy(String recordedBy) {
        this.recordedBy = recordedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
