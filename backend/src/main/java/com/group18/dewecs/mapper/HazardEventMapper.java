package com.group18.dewecs.mapper;

import com.group18.dewecs.domain.HazardEvent;
import com.group18.dewecs.dto.HazardEventResponse;
import com.group18.dewecs.dto.HazardReviewResponse;
import com.group18.dewecs.service.HazardEventService.HazardReview;
import com.group18.dewecs.service.HazardEventService.HazardSummary;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HazardEventMapper {

    private final GroundReportMapper groundReportMapper;
    private final WarningMapper warningMapper;

    public HazardEventMapper(GroundReportMapper groundReportMapper, WarningMapper warningMapper) {
        this.groundReportMapper = groundReportMapper;
        this.warningMapper = warningMapper;
    }

    public HazardEventResponse toResponse(HazardEvent event) {
        return toResponse(event, 0, 0);
    }

    public List<HazardEventResponse> toResponseList(List<HazardEvent> events) {
        return events.stream().map(this::toResponse).toList();
    }

    public HazardEventResponse toResponse(HazardSummary summary) {
        return toResponse(summary.event(), summary.verifiedReports(), summary.liveWarnings());
    }

    public List<HazardEventResponse> toSummaryList(List<HazardSummary> summaries) {
        return summaries.stream().map(this::toResponse).toList();
    }

    public HazardReviewResponse toReview(HazardReview review) {
        return new HazardReviewResponse(
                toResponse(review.event(), review.verifiedReports().size(),
                        (int) review.warnings().stream().filter(w -> isLive(w.getStatus().name())).count()),
                groundReportMapper.toResponseList(review.verifiedReports()),
                warningMapper.toResponseList(review.warnings()));
    }

    private boolean isLive(String status) {
        return status.equals("ISSUED") || status.equals("UPDATED");
    }

    private HazardEventResponse toResponse(HazardEvent event, int verifiedReports, int liveWarnings) {
        return new HazardEventResponse(
                event.getId(),
                event.getHazardType().name(),
                event.getSeverityLevel().name(),
                event.getStatus().name(),
                event.getDistrict().getName(),
                event.getDistrict().getId(),
                event.getOccurredAt(),
                verifiedReports,
                liveWarnings);
    }
}
