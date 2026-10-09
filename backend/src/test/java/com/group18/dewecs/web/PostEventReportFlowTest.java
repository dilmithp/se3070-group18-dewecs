package com.group18.dewecs.web;

import com.group18.dewecs.domain.Citizen;
import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.HazardEvent;
import com.group18.dewecs.domain.MetricType;
import com.group18.dewecs.domain.PostEventReport;
import com.group18.dewecs.domain.Severity;
import com.group18.dewecs.domain.Shelter;
import com.group18.dewecs.domain.User;
import com.group18.dewecs.domain.Warning;
import com.group18.dewecs.domain.WarningStatus;
import com.group18.dewecs.repository.PostEventReportRepository;
import com.group18.dewecs.repository.ReportMetricRepository;
import com.group18.dewecs.repository.WarningRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PostEventReportFlowTest extends FlowTestSupport {

    @Autowired
    private PostEventReportRepository reportRepository;
    @Autowired
    private ReportMetricRepository metricRepository;
    @Autowired
    private WarningRepository warningRepository;

    private Warning warning(HazardEvent event, User issuer, WarningStatus status, int minutesAfterStart) {
        Warning w = new Warning();
        w.setHazardEvent(event);
        w.setSeverity(Severity.HIGH);
        w.setStatus(status);
        w.setMessage("Flood warning");
        w.setIssuedBy(issuer);
        w.setIssuedAt(event.getOccurredAt().plusMinutes(minutesAfterStart));
        return warningRepository.save(w);
    }

    @Test
    void generatingAReportStoresMetricsAndShowsThemOnTheDetailPage() throws Exception {
        District d = district();
        HazardEvent event = hazardEvent(d);
        User officer = officer(d);
        Shelter shelter = shelter(d, organization(), 100);
        shelter.setCurrentOccupancy(30);
        shelterRepository.save(shelter);
        Citizen citizen = new Citizen();
        citizen.setFullName("Citizen One");
        citizen.setPhone("0770000000");
        citizen.setDistrict(d);
        citizen.setNic("200012345678");
        citizenRepository.save(citizen);
        warning(event, officer, WarningStatus.ISSUED, 20);
        warning(event, officer, WarningStatus.DRAFT, 5);
        warning(event, officer, WarningStatus.CANCELLED, 5);

        mvc.perform(post("/post-event-reports").param("hazardEventId", event.getId().toString()))
                .andExpect(flash().attribute("message", "Post-event report generated."));

        PostEventReport report = reportRepository.findAll().get(0);
        assertThat(report.getRelatedWarnings()).hasSize(1);
        assertThat(report.getRelatedShelters()).containsExactly(shelter);
        var metrics = metricRepository.findByReport_IdOrderByMetricType(report.getId());
        assertThat(metrics).extracting(m -> m.getMetricType())
                .containsExactlyInAnyOrder(MetricType.ALERT_TIMELINE, MetricType.CITIZENS_REACHED,
                        MetricType.SHELTER_OCCUPANCY, MetricType.RESOURCE_DISTRIBUTION);
        assertThat(metrics).extracting(m -> m.getValue()).containsExactlyInAnyOrder(20.0, 1.0, 30.0, 0.0);

        String html = mvc.perform(get("/post-event-reports/" + report.getId())).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Alert timeline").contains("20 min").contains("30.0 %")
                .contains("Flood warning").contains("Test Shelter (30/100)");
    }

    @Test
    void generatingTwiceKeepsBothSnapshots() throws Exception {
        HazardEvent event = hazardEvent(district());

        mvc.perform(post("/post-event-reports").param("hazardEventId", event.getId().toString()));
        mvc.perform(post("/post-event-reports").param("hazardEventId", event.getId().toString()));

        assertThat(reportRepository.count()).isEqualTo(2);
    }

    @Test
    void generatingWithoutAnEventAsksForOne() throws Exception {
        mvc.perform(post("/post-event-reports"))
                .andExpect(flash().attribute("error", "Choose a hazard event to report on."));

        assertThat(reportRepository.count()).isZero();
    }

    @Test
    void unknownEventAndUnknownReportAreNotFound() throws Exception {
        mvc.perform(post("/post-event-reports").param("hazardEventId", "999999")).andExpect(status().isNotFound());
        mvc.perform(get("/post-event-reports/999999")).andExpect(status().isNotFound());
    }

    @Test
    void listPageShowsTheEventChoiceAndGeneratedReports() throws Exception {
        HazardEvent event = hazardEvent(district());
        mvc.perform(post("/post-event-reports").param("hazardEventId", event.getId().toString()));

        String html = mvc.perform(get("/post-event-reports")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("Generate report").contains("FLOOD in Test District").contains("Flood");
    }
}
