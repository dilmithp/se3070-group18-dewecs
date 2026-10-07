package com.group18.dewecs.web;

import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.GroundReport;
import com.group18.dewecs.domain.GroundReportStatus;
import com.group18.dewecs.domain.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GroundReportFlowTest extends FlowTestSupport {

    @Test
    void reviewThenActionWithANoteCompletesTheWorkflow() throws Exception {
        District d = district();
        User officer = officer(d);
        GroundReport report = groundReport(d, GroundReportStatus.PENDING_REVIEW);

        mvc.perform(post("/ground-reports/" + report.getId() + "/review")
                        .param("reviewingUserId", officer.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("message", "Report marked reviewed."));
        assertThat(report.getStatus()).isEqualTo(GroundReportStatus.VERIFIED);
        assertThat(report.getVerifiedBy()).isEqualTo(officer);

        mvc.perform(post("/ground-reports/" + report.getId() + "/action")
                        .param("reviewingUserId", officer.getId().toString())
                        .param("note", "Team dispatched"))
                .andExpect(flash().attribute("message", "Report actioned."));
        assertThat(report.getStatus()).isEqualTo(GroundReportStatus.ACTIONED);
        assertThat(report.getActionNote()).isEqualTo("Team dispatched");
    }

    @Test
    void actionWithoutANoteIsRejected() throws Exception {
        District d = district();
        User officer = officer(d);
        GroundReport report = groundReport(d, GroundReportStatus.VERIFIED);

        mvc.perform(post("/ground-reports/" + report.getId() + "/action")
                        .param("reviewingUserId", officer.getId().toString())
                        .param("note", "  "))
                .andExpect(flash().attribute("error", "An action note is required."));
        assertThat(report.getStatus()).isEqualTo(GroundReportStatus.VERIFIED);
    }

    @Test
    void unreviewedReportCannotBeActioned() throws Exception {
        District d = district();
        User officer = officer(d);
        GroundReport report = groundReport(d, GroundReportStatus.PENDING_REVIEW);

        mvc.perform(post("/ground-reports/" + report.getId() + "/action")
                        .param("reviewingUserId", officer.getId().toString())
                        .param("note", "x"))
                .andExpect(flash().attribute("error", "Only reviewed reports can be actioned."));
    }

    @Test
    void dismissRejectsTheReportAndCannotBeRepeated() throws Exception {
        District d = district();
        User officer = officer(d);
        GroundReport report = groundReport(d, GroundReportStatus.PENDING_REVIEW);

        mvc.perform(post("/ground-reports/" + report.getId() + "/dismiss")
                        .param("reviewingUserId", officer.getId().toString()))
                .andExpect(flash().attribute("message", "Report dismissed."));
        assertThat(report.getStatus()).isEqualTo(GroundReportStatus.REJECTED);

        mvc.perform(post("/ground-reports/" + report.getId() + "/dismiss")
                        .param("reviewingUserId", officer.getId().toString()))
                .andExpect(flash().attribute("error", "This report has already been actioned or dismissed."));
    }

    @Test
    void detailPageShowsAnApiPhotoAsAnImageAndALegacyUrlAsText() throws Exception {
        District d = district();
        GroundReport withPhoto = groundReport(d, GroundReportStatus.PENDING_REVIEW);
        withPhoto.setPhotoUrl("/api/v1/photos/123e4567-e89b-12d3-a456-426614174000.png");
        GroundReport legacy = groundReport(d, GroundReportStatus.PENDING_REVIEW);
        legacy.setPhotoUrl("legacy-file.jpg");

        String html = mvc.perform(get("/ground-reports/" + withPhoto.getId())).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("<img class=\"report-photo\"")
                .contains("src=\"/api/v1/photos/123e4567-e89b-12d3-a456-426614174000.png\"")
                .contains("alt=\"Photo attached to this ground report\"");

        String legacyHtml = mvc.perform(get("/ground-reports/" + legacy.getId())).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(legacyHtml).doesNotContain("<img").contains("legacy-file.jpg");
    }
}
