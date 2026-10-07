package com.group18.dewecs.mapper;

import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.GroundReport;
import com.group18.dewecs.domain.GroundReportStatus;
import com.group18.dewecs.domain.HazardType;
import com.group18.dewecs.dto.api.CitizenResponse;
import com.group18.dewecs.dto.api.ReferenceDataResponse;
import com.group18.dewecs.dto.api.ReportPageResponse;
import com.group18.dewecs.dto.api.ReportResponse;
import com.group18.dewecs.service.CitizenService.IdentifyResult;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** Entity to contract-v1 JSON. Separate from GroundReportMapper, whose response carries reporter and reviewer names. */
@Component
public class GroundReportApiMapper {

    public ReportResponse toReport(GroundReport report) {
        return new ReportResponse(
                report.getId(),
                report.getReportedBy().getId(),
                report.getDistrict().getId(),
                report.getDistrict().getName(),
                report.getCategory().name(),
                report.getDescription(),
                report.getGpsLat(),
                report.getGpsLng(),
                report.getPhotoUrl(),
                report.getStatus().name(),
                report.getStatus() == GroundReportStatus.ACTIONED ? report.getActionNote() : null,
                report.getSubmittedAt().truncatedTo(ChronoUnit.MILLIS));
    }

    public ReportPageResponse toPage(Page<GroundReport> page) {
        return new ReportPageResponse(
                page.getContent().stream().map(this::toReport).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    public CitizenResponse toCitizen(IdentifyResult result) {
        var citizen = result.citizen();
        return new CitizenResponse(citizen.getId(), citizen.getFullName(), citizen.getDistrict().getId(),
                citizen.getDistrict().getName(), result.created());
    }

    public ReferenceDataResponse toReferenceData(List<District> districts) {
        return new ReferenceDataResponse(
                districts.stream()
                        .sorted(Comparator.comparing(District::getName, String.CASE_INSENSITIVE_ORDER))
                        .map(d -> new ReferenceDataResponse.DistrictItem(d.getId(), d.getName()))
                        .toList(),
                Arrays.stream(HazardType.values()).map(Enum::name).toList());
    }
}
