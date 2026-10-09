package com.group18.dewecs.mapper;

import com.group18.dewecs.domain.HazardEvent;
import com.group18.dewecs.domain.PostEventReport;
import com.group18.dewecs.domain.ReportMetric;
import com.group18.dewecs.domain.Shelter;
import com.group18.dewecs.domain.Warning;
import com.group18.dewecs.dto.PostEventReportResponse;
import com.group18.dewecs.dto.PostEventReportResponse.MetricLine;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class PostEventReportMapper {

    public PostEventReportResponse toResponse(PostEventReport report, List<ReportMetric> metrics) {
        HazardEvent event = report.getHazardEvent();
        return new PostEventReportResponse(
                report.getId(),
                report.getDistrict().getName(),
                event.getHazardType().name(),
                event.getSeverityLevel().name(),
                event.getStatus().name(),
                event.getOccurredAt(),
                report.getGeneratedAt(),
                report.getRelatedWarnings().stream()
                        .sorted(Comparator.comparing(Warning::getId))
                        .map(w -> "#" + w.getId() + " (" + w.getStatus().name() + ", " + w.getSeverity().name() + "): "
                                + w.getMessage())
                        .toList(),
                report.getRelatedShelters().stream()
                        .sorted(Comparator.comparing(Shelter::getId))
                        .map(s -> s.getName() + " (" + s.getCurrentOccupancy() + "/" + s.getCapacity() + ")")
                        .toList(),
                metrics.stream().map(this::toMetricLine).toList());
    }

    /** For the list page: no warning or shelter lines, because those collections are lazy (open-in-view is off). */
    public PostEventReportResponse toSummary(PostEventReport report) {
        HazardEvent event = report.getHazardEvent();
        return new PostEventReportResponse(
                report.getId(),
                report.getDistrict().getName(),
                event.getHazardType().name(),
                event.getSeverityLevel().name(),
                event.getStatus().name(),
                event.getOccurredAt(),
                report.getGeneratedAt(),
                List.of(),
                List.of(),
                List.of());
    }

    public List<PostEventReportResponse> toSummaryList(List<PostEventReport> reports) {
        return reports.stream().map(this::toSummary).toList();
    }

    private MetricLine toMetricLine(ReportMetric metric) {
        double v = metric.getValue();
        return switch (metric.getMetricType()) {
            case ALERT_TIMELINE -> new MetricLine("Alert timeline", whole(v) + " min",
                    "Minutes from the start of the event to the first published warning (negative: warned in advance).");
            case CITIZENS_REACHED -> new MetricLine("Citizens reached", whole(v),
                    "Citizens registered in the region, counted when at least one warning was published.");
            case SHELTER_OCCUPANCY -> new MetricLine("Shelter occupancy", v + " %",
                    "Occupants over capacity across the region's shelters when the report was generated.");
            case RESOURCE_DISTRIBUTION -> new MetricLine("Resources distributed", whole(v) + " units",
                    "Units delivered to the region's shelters since the event started.");
        };
    }

    private String whole(double v) {
        return String.valueOf(Math.round(v));
    }
}
