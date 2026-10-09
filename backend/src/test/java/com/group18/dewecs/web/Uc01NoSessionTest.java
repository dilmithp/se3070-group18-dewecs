package com.group18.dewecs.web;

import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.HazardEvent;
import com.group18.dewecs.domain.HazardEventStatus;
import com.group18.dewecs.domain.HazardType;
import com.group18.dewecs.domain.RiverBasin;
import com.group18.dewecs.domain.Severity;
import com.group18.dewecs.domain.User;
import com.group18.dewecs.domain.Warning;
import com.group18.dewecs.repository.AlertDeliveryLogRepository;
import com.group18.dewecs.repository.DistrictRepository;
import com.group18.dewecs.repository.HazardEventRepository;
import com.group18.dewecs.repository.RiverBasinRepository;
import com.group18.dewecs.repository.UserRepository;
import com.group18.dewecs.repository.WarningRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression: production runs with open-in-view=false, so no page may read a lazy association after the service call.
 * This class has NO test transaction (each request gets its own, like in production) and cleans up after itself.
 * It loads every page the UC-01 work touched, as HTML and as JSON.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Uc01NoSessionTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private DistrictRepository districts;
    @Autowired
    private UserRepository users;
    @Autowired
    private HazardEventRepository events;
    @Autowired
    private RiverBasinRepository basins;
    @Autowired
    private WarningRepository warnings;
    @Autowired
    private AlertDeliveryLogRepository logs;

    @AfterEach
    void cleanUp() {
        logs.deleteAll();
        warnings.deleteAll();
        events.deleteAll();
        basins.deleteAll();
        users.deleteAll();
        districts.deleteAll();
    }

    private District district(String name) {
        District d = new District();
        d.setName(name);
        return districts.save(d);
    }

    @Test
    void everyUc01PageLoadsWithoutAnOpenSession() throws Exception {
        District galle = district("Galle");
        District matara = district("Matara");
        RiverBasin nilwala = new RiverBasin();
        nilwala.setName("Nilwala Ganga");
        nilwala.setDistricts(new HashSet<>(Set.of(galle, matara)));
        basins.save(nilwala);
        User officer = new User();
        officer.setFullName("Officer");
        officer.setPhone("0710000001");
        officer.setDistrict(galle);
        users.save(officer);
        HazardEvent event = new HazardEvent();
        event.setHazardType(HazardType.FLOOD);
        event.setSeverityLevel(Severity.HIGH);
        event.setStatus(HazardEventStatus.ACTIVE);
        event.setDistrict(galle);
        event.setOccurredAt(LocalDateTime.now().minusHours(2));
        events.save(event);

        mvc.perform(post("/warnings")
                        .param("hazardEventId", event.getId().toString())
                        .param("issuedByUserId", officer.getId().toString())
                        .param("severity", "HIGH")
                        .param("message", "Nilwala is rising")
                        .param("broadcastChannels", "SMS", "RADIO")
                        .param("riverBasinId", nilwala.getId().toString()))
                .andExpect(status().is3xxRedirection());
        Warning warning = warnings.findAll().get(0);
        mvc.perform(post("/warnings/" + warning.getId() + "/publish")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/warnings/" + warning.getId() + "/escalate").param("severity", "CRITICAL"))
                .andExpect(status().is3xxRedirection());

        String detail = mvc.perform(get("/warnings/" + warning.getId())).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(detail).contains("Galle, Matara").contains("Delivery log").contains("Escalation");
        String list = mvc.perform(get("/warnings")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(list).contains("map-marker sev-critical").contains("Galle, Matara");
        mvc.perform(get("/warnings").param("districtId", matara.getId().toString())).andExpect(status().isOk());

        for (String url : new String[] {"/warnings/new?hazardEventId=" + event.getId(), "/hazard-events",
                "/hazard-events/new", "/hazard-events/" + event.getId(), "/dashboard", "/warnings/" + warning.getId(),
                "/warnings"}) {
            mvc.perform(get(url)).andExpect(status().isOk());
            mvc.perform(get(url).accept("application/json")).andExpect(status().isOk());
        }
        mvc.perform(post("/warnings/" + warning.getId() + "/rebroadcast")).andExpect(status().is3xxRedirection());
    }

    @Test
    void theEditFormOfADraftWithABasinLoadsWithoutAnOpenSession() throws Exception {
        District galle = district("Galle");
        User officer = new User();
        officer.setFullName("Officer");
        officer.setPhone("0710000001");
        officer.setDistrict(galle);
        users.save(officer);
        HazardEvent event = new HazardEvent();
        event.setHazardType(HazardType.FLOOD);
        event.setSeverityLevel(Severity.LOW);
        event.setStatus(HazardEventStatus.ACTIVE);
        event.setDistrict(galle);
        event.setOccurredAt(LocalDateTime.now());
        events.save(event);
        mvc.perform(post("/warnings")
                        .param("hazardEventId", event.getId().toString())
                        .param("issuedByUserId", officer.getId().toString())
                        .param("severity", "LOW")
                        .param("message", "Draft"))
                .andExpect(status().is3xxRedirection());
        Warning draft = warnings.findAll().get(0);

        String edit = mvc.perform(get("/warnings/" + draft.getId() + "/edit")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(edit).contains("Currently covers: Galle").contains("keep the current districts");
    }
}
