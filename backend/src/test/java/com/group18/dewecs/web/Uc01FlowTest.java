package com.group18.dewecs.web;

import com.group18.dewecs.domain.AlertDeliveryLog;
import com.group18.dewecs.domain.DeliveryOutcome;
import com.group18.dewecs.domain.DeliveryTrigger;
import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.GroundReport;
import com.group18.dewecs.domain.GroundReportStatus;
import com.group18.dewecs.domain.HazardEvent;
import com.group18.dewecs.domain.HazardEventStatus;
import com.group18.dewecs.domain.RiverBasin;
import com.group18.dewecs.domain.Severity;
import com.group18.dewecs.domain.User;
import com.group18.dewecs.domain.Warning;
import com.group18.dewecs.domain.WarningStatus;
import com.group18.dewecs.repository.AlertDeliveryLogRepository;
import com.group18.dewecs.repository.RiverBasinRepository;
import com.group18.dewecs.repository.WarningRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** UC-01 end to end through the pages: register an event, review evidence, draft, publish, broadcast, escalate. */
class Uc01FlowTest extends FlowTestSupport {

    @Autowired
    private WarningRepository warningRepository;
    @Autowired
    private RiverBasinRepository riverBasinRepository;
    @Autowired
    private AlertDeliveryLogRepository logRepository;

    private Warning draft(HazardEvent event, User officer, Long riverBasinId, String... channels) throws Exception {
        var request = post("/warnings")
                .param("hazardEventId", event.getId().toString())
                .param("issuedByUserId", officer.getId().toString())
                .param("severity", "HIGH")
                .param("message", "Move to higher ground");
        if (channels.length > 0) {
            request.param("broadcastChannels", channels);
        }
        if (riverBasinId != null) {
            request.param("riverBasinId", riverBasinId.toString());
        }
        mvc.perform(request).andExpect(status().is3xxRedirection());
        List<Warning> all = warningRepository.findAll();
        return all.get(all.size() - 1);
    }

    private Warning published(HazardEvent event, User officer, Long riverBasinId, String... channels) throws Exception {
        Warning w = draft(event, officer, riverBasinId, channels);
        mvc.perform(post("/warnings/" + w.getId() + "/publish"))
                .andExpect(flash().attribute("message", "Warning published."));
        return w;
    }

    private RiverBasin basin(String name, District... districts) {
        RiverBasin b = new RiverBasin();
        b.setName(name);
        b.setDistricts(new HashSet<>(Set.of(districts)));
        return riverBasinRepository.save(b);
    }

    // ---------- hazard events ----------

    @Test
    void registeringAHazardEventStartsItActiveAndItShowsInTheList() throws Exception {
        District d = district();

        mvc.perform(post("/hazard-events").param("hazardType", "FLOOD").param("severity", "HIGH")
                        .param("districtId", d.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("message", "Hazard event registered."));

        HazardEvent saved = hazardEventRepository.findAll().get(0);
        assertThat(saved.getStatus()).isEqualTo(HazardEventStatus.ACTIVE);
        String list = mvc.perform(get("/hazard-events")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(list).contains("Flood").contains("Test District").contains("Register hazard event");
    }

    @Test
    void anIncompleteOrFutureHazardEventFormIsShownAgain() throws Exception {
        mvc.perform(post("/hazard-events").param("severity", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(view().name("hazard-events/form"));

        District d = district();
        mvc.perform(post("/hazard-events").param("hazardType", "FLOOD").param("severity", "HIGH")
                        .param("districtId", d.getId().toString()).param("occurredAt", "2099-01-01T10:00"))
                .andExpect(status().isOk())
                .andExpect(view().name("hazard-events/form"));

        assertThat(hazardEventRepository.count()).isZero();
    }

    @Test
    void aHazardEventOnlyMovesForward() throws Exception {
        HazardEvent event = hazardEvent(district());

        mvc.perform(post("/hazard-events/" + event.getId() + "/status").param("status", "CONTAINED"))
                .andExpect(flash().attribute("message", "Hazard event is now CONTAINED."));
        assertThat(event.getStatus()).isEqualTo(HazardEventStatus.CONTAINED);

        mvc.perform(post("/hazard-events/" + event.getId() + "/status").param("status", "ACTIVE"))
                .andExpect(flash().attribute("error", "An event cannot go back from CONTAINED to ACTIVE."));
        assertThat(event.getStatus()).isEqualTo(HazardEventStatus.CONTAINED);
    }

    @Test
    void theEventPageAndTheWarningFormShowOnlyVerifiedFieldEvidence() throws Exception {
        District d = district();
        HazardEvent event = hazardEvent(d);
        GroundReport verified = groundReport(d, GroundReportStatus.VERIFIED);
        verified.setDescription("Verified: the bridge road is under water");
        GroundReport pending = groundReport(d, GroundReportStatus.PENDING_REVIEW);
        pending.setDescription("Unverified rumour about a dam");

        String eventPage = mvc.perform(get("/hazard-events/" + event.getId())).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(eventPage).contains("Verified: the bridge road is under water")
                .doesNotContain("Unverified rumour").contains("Issue a warning for this event");

        String form = mvc.perform(get("/warnings/new").param("hazardEventId", event.getId().toString()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(form).contains("Hazard information").contains("Verified: the bridge road is under water")
                .doesNotContain("Unverified rumour");

        String dashboard = mvc.perform(get("/dashboard")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(dashboard).contains("Active hazard events").contains("Issue warning");
    }

    // ---------- river basin ----------

    @Test
    void aBasinWarningCoversEveryDistrictAndShowsOnTheMapAndInEachDistrictFilter() throws Exception {
        District colombo = district();
        District gampaha = new District();
        gampaha.setName("Gampaha");
        districtRepository.save(gampaha);
        RiverBasin kelani = basin("Kelani Ganga", colombo, gampaha);
        HazardEvent event = hazardEvent(colombo);
        User officer = officer(colombo);

        Warning w = published(event, officer, kelani.getId(), "SMS");

        assertThat(w.effectiveDistricts()).extracting(District::getName).containsExactlyInAnyOrder("Test District", "Gampaha");
        String detail = mvc.perform(get("/warnings/" + w.getId())).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(detail).contains("Regions covered").contains("Gampaha").contains("Test District");

        String forGampaha = mvc.perform(get("/warnings").param("districtId", gampaha.getId().toString()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(forGampaha).contains("Gampaha, Test District");

        // The map has no coordinates for these made-up district names, so it shows the empty-map text.
        assertThat(mvc.perform(get("/warnings")).andReturn().getResponse().getContentAsString())
                .contains("Active warnings map");
    }

    @Test
    void theMapDrawsAMarkerForADistrictWithALiveWarning() throws Exception {
        District galle = new District();
        galle.setName("Galle");
        districtRepository.save(galle);
        HazardEvent event = hazardEvent(galle);
        published(event, officer(galle), null);

        String html = mvc.perform(get("/warnings")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("sl-map").contains("class=\"map-marker sev-high\"").contains("Galle")
                .contains("1 live warning(s)");
    }

    // ---------- broadcast and delivery log ----------

    @Test
    void publishingBroadcastsOnEveryChannelAndTheLogIsShownOnTheWarningPage() throws Exception {
        District d = district();
        Warning w = published(hazardEvent(d), officer(d), null, "SMS", "RADIO");

        List<AlertDeliveryLog> rows = logRepository.findByWarning_IdOrderByCreatedAtAscIdAsc(w.getId());
        assertThat(rows).hasSize(2).allSatisfy(r -> {
            assertThat(r.getOutcome()).isEqualTo(DeliveryOutcome.SENT);
            assertThat(r.getTrigger()).isEqualTo(DeliveryTrigger.PUBLISH);
        });
        String page = mvc.perform(get("/warnings/" + w.getId())).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(page).contains("Delivery log").contains("Sms").contains("Radio").contains("Sent")
                .doesNotContain("At least one channel has not been delivered yet");
    }

    @Test
    void aDraftHasNoDeliveryLogYetAndAWarningWithoutChannelsSaysNothingWasSent() throws Exception {
        District d = district();
        Warning w = draft(hazardEvent(d), officer(d), null);

        assertThat(mvc.perform(get("/warnings/" + w.getId())).andReturn().getResponse().getContentAsString())
                .contains("No broadcast channel was chosen");
        mvc.perform(post("/warnings/" + w.getId() + "/publish"));
        assertThat(logRepository.count()).isZero();
    }

    @Test
    void sendingAgainWhenEveryChannelWasDeliveredDoesNothing() throws Exception {
        District d = district();
        Warning w = published(hazardEvent(d), officer(d), null, "SMS");

        mvc.perform(post("/warnings/" + w.getId() + "/rebroadcast"))
                .andExpect(flash().attribute("message", "Every channel is already delivered. Nothing to send again."));
        assertThat(logRepository.count()).isEqualTo(1);
    }

    @Test
    void aDraftCannotBeSentAgain() throws Exception {
        District d = district();
        Warning w = draft(hazardEvent(d), officer(d), null, "SMS");

        mvc.perform(post("/warnings/" + w.getId() + "/rebroadcast"))
                .andExpect(flash().attribute("error", "Only published warnings can be sent again."));
    }

    // ---------- escalation ----------

    @Test
    void escalatingRaisesTheSeverityAndSendsAgainAndStillCountsAsOpen() throws Exception {
        District d = district();
        Warning w = published(hazardEvent(d), officer(d), null, "SMS");
        var issuedAt = w.getIssuedAt();

        mvc.perform(post("/warnings/" + w.getId() + "/escalate").param("severity", "CRITICAL")
                        .param("note", "Water above the danger mark"))
                .andExpect(flash().attribute("message", "Warning escalated to CRITICAL and sent again."));

        assertThat(w.getSeverity()).isEqualTo(Severity.CRITICAL);
        assertThat(w.getStatus()).isEqualTo(WarningStatus.UPDATED);
        assertThat(w.getIssuedAt()).isEqualTo(issuedAt);
        assertThat(w.getMessage()).contains("[Escalated to CRITICAL] Water above the danger mark");
        List<AlertDeliveryLog> rows = logRepository.findByWarning_IdOrderByCreatedAtAscIdAsc(w.getId());
        assertThat(rows).extracting(AlertDeliveryLog::getTrigger)
                .containsExactly(DeliveryTrigger.PUBLISH, DeliveryTrigger.ESCALATION);
        assertThat(rows.get(1).getDetail()).startsWith("Escalated from HIGH to CRITICAL.");

        mvc.perform(get("/dashboard").accept("application/json"))
                .andExpect(jsonPath("$.summary.openWarnings").value(1));
        String page = mvc.perform(get("/warnings/" + w.getId())).andReturn().getResponse().getContentAsString();
        assertThat(page).contains("Updated").doesNotContain("Escalate this warning");
    }

    @Test
    void escalationNeedsAHigherSeverityAndAPublishedWarning() throws Exception {
        District d = district();
        Warning w = published(hazardEvent(d), officer(d), null);

        mvc.perform(post("/warnings/" + w.getId() + "/escalate").param("severity", "LOW"))
                .andExpect(flash().attribute("error", "Escalation must raise the severity (it is HIGH now)."));
        mvc.perform(post("/warnings/" + w.getId() + "/escalate"))
                .andExpect(flash().attribute("error", "Choose the higher severity to escalate to."));
        assertThat(w.getStatus()).isEqualTo(WarningStatus.ISSUED);
        assertThat(w.getSeverity()).isEqualTo(Severity.HIGH);

        Warning notPublished = draft(w.getHazardEvent(), w.getIssuedBy(), null);
        mvc.perform(post("/warnings/" + notPublished.getId() + "/escalate").param("severity", "CRITICAL"))
                .andExpect(flash().attribute("error", "Only published warnings can be escalated."));
    }

    @Test
    void theEscalatePanelOffersOnlyHigherSeveritiesAndIsReachableFromTheList() throws Exception {
        District d = district();
        Warning w = published(hazardEvent(d), officer(d), null);

        String page = mvc.perform(get("/warnings/" + w.getId())).andReturn().getResponse().getContentAsString();
        assertThat(page).contains("Escalate this warning").contains("value=\"CRITICAL\"")
                .doesNotContain("value=\"LOW\"").doesNotContain("value=\"MODERATE\"");
        String list = mvc.perform(get("/warnings")).andReturn().getResponse().getContentAsString();
        assertThat(list).contains("#escalate-h");
    }

    @Test
    void anEscalatedWarningCanBeRetracted() throws Exception {
        District d = district();
        Warning w = published(hazardEvent(d), officer(d), null);
        mvc.perform(post("/warnings/" + w.getId() + "/escalate").param("severity", "CRITICAL"));

        mvc.perform(post("/warnings/" + w.getId() + "/retract"))
                .andExpect(flash().attribute("message", "Warning retracted."));
        assertThat(w.getStatus()).isEqualTo(WarningStatus.CANCELLED);
    }

    // ---------- JSON ----------

    @Test
    void theNewActionsWorkAsJsonToo() throws Exception {
        District d = district();
        User officer = officer(d);
        String json = "application/json";

        mvc.perform(post("/hazard-events").contentType(json).accept(json)
                        .content("{\"hazardType\":\"FLOOD\",\"severity\":\"HIGH\",\"districtId\":" + d.getId() + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Hazard event registered."));
        HazardEvent event = hazardEventRepository.findAll().get(0);

        mvc.perform(post("/warnings").contentType(json).accept(json)
                        .content("{\"hazardEventId\":" + event.getId() + ",\"issuedByUserId\":" + officer.getId()
                                + ",\"severity\":\"HIGH\",\"message\":\"Evacuate\",\"broadcastChannels\":[\"SMS\"]}"))
                .andExpect(status().isCreated());
        Warning w = warningRepository.findAll().get(0);
        mvc.perform(post("/warnings/" + w.getId() + "/publish").accept(json)).andExpect(status().isOk());

        mvc.perform(post("/warnings/" + w.getId() + "/escalate").contentType(json).accept(json)
                        .content("{\"severity\":\"CRITICAL\",\"note\":\"Rising fast\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Warning escalated to CRITICAL and sent again."));
        mvc.perform(post("/warnings/" + w.getId() + "/escalate").contentType(json).accept(json)
                        .content("{\"severity\":\"HIGH\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        mvc.perform(get("/warnings/" + w.getId()).accept(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warning.status").value("UPDATED"))
                .andExpect(jsonPath("$.deliveryLog.length()").value(2))
                .andExpect(jsonPath("$.allDelivered").value(true));
        mvc.perform(get("/warnings").accept(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.map.markers").isArray());
    }
}
