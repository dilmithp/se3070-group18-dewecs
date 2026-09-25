package com.group18.dewecs.domain;

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

@Entity
@Table(name = "report_metrics")
public class ReportMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "report_id")
    private PostEventReport report;

    @NotNull
    @Enumerated(EnumType.STRING)
    private MetricType metricType;

    @NotNull
    private Double value;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PostEventReport getReport() {
        return report;
    }

    public void setReport(PostEventReport report) {
        this.report = report;
    }

    public MetricType getMetricType() {
        return metricType;
    }

    public void setMetricType(MetricType metricType) {
        this.metricType = metricType;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }
}
