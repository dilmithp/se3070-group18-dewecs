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

/**
 * One performance indicator of a report (relief per capita, response latency, an organisation's share, needed versus
 * distributed per supply type ...). The fixed MetricType enum of the shared model cannot hold these, hence this table.
 */
@Entity
@Table(name = "report_kpis")
public class ReportKpi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "report_id")
    private PostEventReport report;

    /** A stable key: relief_per_capita, response_latency, share, need, distributed, ... */
    @NotNull
    @Column(length = 50)
    private String kpiKey;

    @NotNull
    @Column(length = 150)
    private String label;

    /** Null when the figure could not be computed (the detail then says why). */
    @Column(name = "kpi_value")
    private Double value;

    @Column(length = 40)
    private String unit;

    @Column(length = 500)
    private String detail;

    private Integer sortOrder = 0;

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

    public String getKpiKey() {
        return kpiKey;
    }

    public void setKpiKey(String kpiKey) {
        this.kpiKey = kpiKey;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
