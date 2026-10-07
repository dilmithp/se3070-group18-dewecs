package com.group18.dewecs.web;

import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.GroundReportStatus;
import com.group18.dewecs.domain.Organization;
import com.group18.dewecs.domain.RescueTeamStatus;
import com.group18.dewecs.domain.Resource;
import com.group18.dewecs.domain.Shelter;
import com.group18.dewecs.domain.User;
import com.group18.dewecs.dto.DashboardSummary;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DashboardFlowTest extends FlowTestSupport {

    private DashboardSummary summary() throws Exception {
        return (DashboardSummary) mvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("summary"))
                .andReturn().getModelAndView().getModel().get("summary");
    }

    @Test
    void emptySystemShowsFiveZeros() throws Exception {
        DashboardSummary s = summary();
        assertThat(s.getOpenWarnings()).isZero();
        assertThat(s.getFullShelters()).isZero();
        assertThat(s.getPendingRescueRequests()).isZero();
        assertThat(s.getLowStockSupplies()).isZero();
        assertThat(s.getUnreviewedReports()).isZero();
    }

    @Test
    void theFiveCountsFollowActionsTakenThroughTheScreens() throws Exception {
        District d = district();
        Organization o = organization();
        User officer = officer(d);
        var event = hazardEvent(d);
        Shelter shelter = shelter(d, o, 1);
        Resource supply = supply(d, o, 12);
        var report = groundReport(d, GroundReportStatus.PENDING_REVIEW);
        team(d, o, RescueTeamStatus.AVAILABLE);

        // open warning: draft is not counted until published
        mvc.perform(post("/warnings")
                .param("hazardEventId", event.getId().toString())
                .param("issuedByUserId", officer.getId().toString())
                .param("severity", "HIGH").param("message", "Flood"));
        Long warningId = warningId();
        assertThat(summary().getOpenWarnings()).isZero();
        mvc.perform(post("/warnings/" + warningId + "/publish"));
        assertThat(summary().getOpenWarnings()).isEqualTo(1);
        mvc.perform(post("/warnings/" + warningId + "/retract"));
        assertThat(summary().getOpenWarnings()).isZero();

        // full shelter
        mvc.perform(post("/shelters/" + shelter.getId() + "/check-in").param("fullName", "A").param("nic", "1"));
        assertThat(summary().getFullShelters()).isEqualTo(1);

        // pending rescue request
        mvc.perform(post("/rescue-requests")
                .param("districtId", d.getId().toString()).param("requesterName", "K")
                .param("requesterPhone", "07").param("description", "help").param("priority", "HIGH"));
        assertThat(summary().getPendingRescueRequests()).isEqualTo(1);

        // low stock: 12 -> distribute 5 -> 7 (< 10)
        assertThat(summary().getLowStockSupplies()).isZero();
        mvc.perform(post("/relief-distributions")
                .param("resourceId", supply.getId().toString())
                .param("shelterId", shelter.getId().toString()).param("quantity", "5"));
        assertThat(summary().getLowStockSupplies()).isEqualTo(1);

        // unreviewed report
        assertThat(summary().getUnreviewedReports()).isEqualTo(1);
        mvc.perform(post("/ground-reports/" + report.getId() + "/review")
                .param("reviewingUserId", officer.getId().toString()));
        assertThat(summary().getUnreviewedReports()).isZero();
    }

    private Long warningId() {
        return warningIdHolder.findAll().get(0).getId();
    }

    @org.springframework.beans.factory.annotation.Autowired
    private com.group18.dewecs.repository.WarningRepository warningIdHolder;
}
