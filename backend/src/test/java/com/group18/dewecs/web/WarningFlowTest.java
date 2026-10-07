package com.group18.dewecs.web;

import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.HazardEvent;
import com.group18.dewecs.domain.Severity;
import com.group18.dewecs.domain.User;
import com.group18.dewecs.domain.Warning;
import com.group18.dewecs.domain.WarningStatus;
import com.group18.dewecs.repository.WarningRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class WarningFlowTest extends FlowTestSupport {

    @Autowired
    private WarningRepository warningRepository;

    private HazardEvent event;
    private User officer;

    private void fixtures() {
        District d = district();
        event = hazardEvent(d);
        officer = officer(d);
    }

    private Warning createDraftViaForm(String message) throws Exception {
        mvc.perform(post("/warnings")
                        .param("hazardEventId", event.getId().toString())
                        .param("issuedByUserId", officer.getId().toString())
                        .param("severity", "HIGH")
                        .param("message", message)
                        .param("broadcastChannels", "SMS", "RADIO"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("message", "Draft warning created."));
        List<Warning> all = warningRepository.findAll();
        return all.get(all.size() - 1);
    }

    @Test
    void createEditPublishAndRetractWalksThroughTheWholeLifecycle() throws Exception {
        fixtures();
        Warning draft = createDraftViaForm("Flood coming");
        assertThat(draft.getStatus()).isEqualTo(WarningStatus.DRAFT);
        assertThat(draft.getIssuedAt()).isNull();
        assertThat(draft.getBroadcastChannels()).hasSize(2);

        mvc.perform(post("/warnings/" + draft.getId())
                        .param("hazardEventId", event.getId().toString())
                        .param("issuedByUserId", officer.getId().toString())
                        .param("severity", "CRITICAL")
                        .param("message", "Evacuate now")
                        .param("broadcastChannels", "SIREN"))
                .andExpect(status().is3xxRedirection());
        assertThat(draft.getSeverity()).isEqualTo(Severity.CRITICAL);
        assertThat(draft.getMessage()).isEqualTo("Evacuate now");

        mvc.perform(post("/warnings/" + draft.getId() + "/publish"))
                .andExpect(status().is3xxRedirection());
        assertThat(draft.getStatus()).isEqualTo(WarningStatus.ISSUED);
        assertThat(draft.getIssuedAt()).isNotNull();

        mvc.perform(post("/warnings/" + draft.getId() + "/retract"))
                .andExpect(status().is3xxRedirection());
        assertThat(draft.getStatus()).isEqualTo(WarningStatus.CANCELLED);
    }

    @Test
    void publishedWarningCannotBeEdited() throws Exception {
        fixtures();
        Warning w = createDraftViaForm("Flood coming");
        mvc.perform(post("/warnings/" + w.getId() + "/publish")).andExpect(status().is3xxRedirection());

        mvc.perform(post("/warnings/" + w.getId())
                        .param("hazardEventId", event.getId().toString())
                        .param("issuedByUserId", officer.getId().toString())
                        .param("severity", "LOW")
                        .param("message", "sneaky edit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "Only draft warnings can be edited or published."));
        assertThat(w.getMessage()).isEqualTo("Flood coming");
    }

    @Test
    void draftWithoutMessageCannotBePublished() throws Exception {
        fixtures();
        Warning w = createDraftViaForm("");
        mvc.perform(post("/warnings/" + w.getId() + "/publish"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "A warning cannot be published without a message."));
        assertThat(w.getStatus()).isEqualTo(WarningStatus.DRAFT);
    }

    @Test
    void createWithoutSeverityReShowsTheForm() throws Exception {
        fixtures();
        mvc.perform(post("/warnings")
                        .param("hazardEventId", event.getId().toString())
                        .param("issuedByUserId", officer.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("warnings/form"));
        assertThat(warningRepository.count()).isZero();
    }

    @Test
    void overdueIssuedWarningFlipsToExpiredWhenTheListIsOpened() throws Exception {
        fixtures();
        Warning w = createDraftViaForm("Short lived");
        w.setExpiresAt(LocalDateTime.now().minusMinutes(5));
        mvc.perform(post("/warnings/" + w.getId() + "/publish")).andExpect(status().is3xxRedirection());
        assertThat(w.getStatus()).isEqualTo(WarningStatus.ISSUED);

        mvc.perform(get("/warnings")).andExpect(status().isOk());
        assertThat(w.getStatus()).isEqualTo(WarningStatus.EXPIRED);
    }
}
