package com.group18.dewecs.mapper;

import com.group18.dewecs.domain.Citizen;
import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.GroundReport;
import com.group18.dewecs.domain.GroundReportStatus;
import com.group18.dewecs.domain.HazardType;
import com.group18.dewecs.dto.api.ReferenceDataResponse;
import com.group18.dewecs.dto.api.ReportResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GroundReportApiMapperTest {

    private final GroundReportApiMapper mapper = new GroundReportApiMapper();

    private District district(String name) {
        District d = new District();
        d.setName(name);
        return d;
    }

    private GroundReport report(GroundReportStatus status) {
        GroundReport r = new GroundReport();
        r.setReportedBy(new Citizen());
        r.setDistrict(district("Colombo"));
        r.setCategory(HazardType.FLOOD);
        r.setDescription("d");
        r.setGpsLat(new BigDecimal("6.9271234"));
        r.setGpsLng(new BigDecimal("79.8612345"));
        r.setStatus(status);
        r.setActionNote("internal note");
        r.setSubmittedAt(LocalDateTime.parse("2026-10-07T14:03:11.123456789"));
        return r;
    }

    @Test
    void submittedAtIsTruncatedToMillisecondsOnOutput() {
        ReportResponse response = mapper.toReport(report(GroundReportStatus.PENDING_REVIEW));

        assertThat(response.submittedAt()).isEqualTo(LocalDateTime.parse("2026-10-07T14:03:11.123"));
    }

    @Test
    void actionNoteIsOnlyReturnedWhenActioned() {
        assertThat(mapper.toReport(report(GroundReportStatus.VERIFIED)).actionNote()).isNull();
        assertThat(mapper.toReport(report(GroundReportStatus.REJECTED)).actionNote()).isNull();
        assertThat(mapper.toReport(report(GroundReportStatus.ACTIONED)).actionNote()).isEqualTo("internal note");
    }

    @Test
    void districtsAreSortedByNameIgnoringCase_andCategoriesAreTheHazardTypes() {
        ReferenceDataResponse response = mapper.toReferenceData(
                List.of(district("kandy"), district("Galle"), district("Colombo"), district("ampara")));

        assertThat(response.districts()).extracting(ReferenceDataResponse.DistrictItem::name)
                .containsExactly("ampara", "Colombo", "Galle", "kandy");
        assertThat(response.categories()).containsExactly("FLOOD", "LANDSLIDE", "CYCLONE", "DROUGHT");
    }
}
