package com.group18.dewecs.dto;

import java.time.LocalDateTime;
import java.util.List;

public class PostEventReportResponse {

    private final Long id;
    private final String districtName;
    private final String hazardType;
    private final String severity;
    private final String eventStatus;
    private final LocalDateTime occurredAt;
    private final LocalDateTime generatedAt;
    private final List<String> warningLines;
    private final List<String> shelterLines;
    private final List<MetricLine> metrics;

    public PostEventReportResponse(Long id, String districtName, String hazardType, String severity,
                                    String eventStatus, LocalDateTime occurredAt, LocalDateTime generatedAt,
                                    List<String> warningLines, List<String> shelterLines, List<MetricLine> metrics) {
        this.id = id;
        this.districtName = districtName;
        this.hazardType = hazardType;
        this.severity = severity;
        this.eventStatus = eventStatus;
        this.occurredAt = occurredAt;
        this.generatedAt = generatedAt;
        this.warningLines = warningLines;
        this.shelterLines = shelterLines;
        this.metrics = metrics;
    }

    public Long getId() {
        return id;
    }

    public String getDistrictName() {
        return districtName;
    }

    public String getHazardType() {
        return hazardType;
    }

    public String getSeverity() {
        return severity;
    }

    public String getEventStatus() {
        return eventStatus;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public List<String> getWarningLines() {
        return warningLines;
    }

    public List<String> getShelterLines() {
        return shelterLines;
    }

    public List<MetricLine> getMetrics() {
        return metrics;
    }

    /** One metric ready to show: a title, the number with its unit, and a one-line explanation. */
    public static class MetricLine {

        private final String title;
        private final String value;
        private final String explanation;

        public MetricLine(String title, String value, String explanation) {
            this.title = title;
            this.value = value;
            this.explanation = explanation;
        }

        public String getTitle() {
            return title;
        }

        public String getValue() {
            return value;
        }

        public String getExplanation() {
            return explanation;
        }
    }
}
