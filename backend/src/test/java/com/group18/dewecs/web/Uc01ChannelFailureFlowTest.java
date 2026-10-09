package com.group18.dewecs.web;

import com.group18.dewecs.domain.AlertDeliveryLog;
import com.group18.dewecs.domain.BroadcastChannel;
import com.group18.dewecs.domain.DeliveryOutcome;
import com.group18.dewecs.domain.DeliveryTrigger;
import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.Warning;
import com.group18.dewecs.domain.WarningStatus;
import com.group18.dewecs.repository.AlertDeliveryLogRepository;
import com.group18.dewecs.repository.WarningRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Channel failure handling (E1) through the real pages: the SMS and siren gateways are made to fail, so publishing
 * must still succeed, the SMS alert falls back to radio, the siren (no fallback) stays undelivered, and the
 * officer can send again.
 */
@TestPropertySource(properties = "dewecs.alerts.simulate-failures=SMS,SIREN")
class Uc01ChannelFailureFlowTest extends FlowTestSupport {

    @Autowired
    private WarningRepository warningRepository;
    @Autowired
    private AlertDeliveryLogRepository logRepository;

    private Warning publishedOn(String... channels) throws Exception {
        District d = district();
        mvc.perform(post("/warnings")
                        .param("hazardEventId", hazardEvent(d).getId().toString())
                        .param("issuedByUserId", officer(d).getId().toString())
                        .param("severity", "HIGH")
                        .param("message", "Evacuate now")
                        .param("broadcastChannels", channels))
                .andExpect(status().is3xxRedirection());
        Warning w = warningRepository.findAll().get(0);
        mvc.perform(post("/warnings/" + w.getId() + "/publish"))
                .andExpect(flash().attribute("message", "Warning published."));
        return w;
    }

    @Test
    void aFailingChannelNeverBlocksPublishingAndFallsBackToRadio() throws Exception {
        Warning w = publishedOn("SMS");

        assertThat(w.getStatus()).isEqualTo(WarningStatus.ISSUED);
        List<AlertDeliveryLog> rows = logRepository.findByWarning_IdOrderByCreatedAtAscIdAsc(w.getId());
        assertThat(rows).extracting(AlertDeliveryLog::getChannel, AlertDeliveryLog::getOutcome,
                        AlertDeliveryLog::getFallbackFor)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(BroadcastChannel.SMS, DeliveryOutcome.FAILED, null),
                        org.assertj.core.groups.Tuple.tuple(BroadcastChannel.SMS, DeliveryOutcome.FAILED, null),
                        org.assertj.core.groups.Tuple.tuple(BroadcastChannel.RADIO, DeliveryOutcome.SENT,
                                BroadcastChannel.SMS));

        String page = mvc.perform(get("/warnings/" + w.getId())).andReturn().getResponse().getContentAsString();
        assertThat(page).contains("Failed").contains("Sent").contains("(instead of sms)")
                .doesNotContain("At least one channel has not been delivered yet");
    }

    @Test
    void aChannelWithNoFallbackStaysUndeliveredAndCanBeSentAgain() throws Exception {
        Warning w = publishedOn("SIREN");

        List<AlertDeliveryLog> rows = logRepository.findByWarning_IdOrderByCreatedAtAscIdAsc(w.getId());
        assertThat(rows).hasSize(2).allSatisfy(r -> assertThat(r.getOutcome()).isEqualTo(DeliveryOutcome.FAILED));
        String page = mvc.perform(get("/warnings/" + w.getId())).andReturn().getResponse().getContentAsString();
        assertThat(page).contains("At least one channel has not been delivered yet")
                .contains("Send again on the failed channels");

        mvc.perform(post("/warnings/" + w.getId() + "/rebroadcast"))
                .andExpect(flash().attribute("message", "Sent again: 0 delivered, 2 failed attempt(s)."));

        List<AlertDeliveryLog> after = logRepository.findByWarning_IdOrderByCreatedAtAscIdAsc(w.getId());
        assertThat(after).hasSize(4);
        assertThat(after.stream().filter(r -> r.getTrigger() == DeliveryTrigger.RETRY)).hasSize(2);
    }

    @Test
    void anEscalationAlsoSurvivesAFailingChannel() throws Exception {
        Warning w = publishedOn("SMS", "TV");

        mvc.perform(post("/warnings/" + w.getId() + "/escalate").param("severity", "CRITICAL"))
                .andExpect(flash().attribute("message", "Warning escalated to CRITICAL and sent again."));

        assertThat(w.getStatus()).isEqualTo(WarningStatus.UPDATED);
        assertThat(logRepository.findByWarning_IdOrderByCreatedAtAscIdAsc(w.getId()).stream()
                .filter(r -> r.getTrigger() == DeliveryTrigger.ESCALATION && r.getOutcome() == DeliveryOutcome.SENT))
                .isNotEmpty();
    }
}
