package com.group18.dewecs.mapper;

import com.group18.dewecs.domain.GroundReport;
import com.group18.dewecs.dto.GroundReportResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GroundReportMapper {

    public GroundReportResponse toResponse(GroundReport report) {
        return new GroundReportResponse(
                report.getId(),
                report.getReportedBy().getFullName(),
                report.getReportedBy().getPhone(),
                report.getDistrict().getName(),
                report.getDistrict().getId(),
                report.getCategory().name(),
                report.getGpsLat(),
                report.getGpsLng(),
                report.getPhotoUrl(),
                report.getDescription(),
                report.getStatus().name(),
                report.getActionNote(),
                report.getVerifiedBy() != null ? report.getVerifiedBy().getFullName() : null,
                report.getSubmittedAt()
        );
    }

    public List<GroundReportResponse> toResponseList(List<GroundReport> reports) {
        return reports.stream().map(this::toResponse).toList();
    }
}
