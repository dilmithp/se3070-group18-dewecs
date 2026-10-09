package com.group18.dewecs.web;

import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.HazardEvent;
import com.group18.dewecs.domain.HazardEventStatus;
import com.group18.dewecs.domain.HazardType;
import com.group18.dewecs.domain.Organization;
import com.group18.dewecs.domain.OrganizationType;
import com.group18.dewecs.domain.PostEventReport;
import com.group18.dewecs.domain.Severity;
import com.group18.dewecs.domain.Shelter;
import com.group18.dewecs.domain.ShelterStatus;
import com.group18.dewecs.domain.User;
import com.group18.dewecs.domain.Warning;
import com.group18.dewecs.domain.WarningStatus;
import com.group18.dewecs.repository.DistrictRepository;
import com.group18.dewecs.repository.HazardEventRepository;
import com.group18.dewecs.repository.OrganizationRepository;
import com.group18.dewecs.repository.PostEventReportRepository;
import com.group18.dewecs.repository.ReportMetricRepository;
import com.group18.dewecs.repository.ShelterRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression: production runs with spring.jpa.open-in-view=false, so a page must not read lazy collections after
 * the service call returned. FlowTestSupport wraps every test in one transaction, which hides that, so this class
 * deliberately has NO test transaction: each request gets its own, like in production. It cleans up after itself.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PostEventReportNoSessionTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private DistrictRepository districts;
    @Autowired
    private OrganizationRepository organizations;
    @Autowired
    private UserRepository users;
    @Autowired
    private HazardEventRepository events;
    @Autowired
    private WarningRepository warnings;
    @Autowired
    private ShelterRepository shelters;
    @Autowired
    private PostEventReportRepository reports;
    @Autowired
    private ReportMetricRepository metrics;
    @Autowired
    private com.group18.dewecs.repository.ReportKpiRepository reportKpis;
    @Autowired
    private com.group18.dewecs.repository.ReportDetailsRepository reportDetails;

    @AfterEach
    void cleanUp() {
        reportKpis.deleteAll();
        reportDetails.deleteAll();
        metrics.deleteAll();
        reports.deleteAll();
        warnings.deleteAll();
        shelters.deleteAll();
        events.deleteAll();
        users.deleteAll();
        organizations.deleteAll();
        districts.deleteAll();
    }

    @Test
    void listAndDetailPagesWorkWithoutAnOpenSession() throws Exception {
        District district = new District();
        district.setName("Galle");
        districts.save(district);
        Organization org = new Organization();
        org.setName("DMC");
        org.setType(OrganizationType.GOVERNMENT);
        organizations.save(org);
        User officer = new User();
        officer.setFullName("Officer");
        officer.setPhone("0710000001");
        officer.setDistrict(district);
        users.save(officer);
        HazardEvent event = new HazardEvent();
        event.setHazardType(HazardType.FLOOD);
        event.setSeverityLevel(Severity.HIGH);
        event.setStatus(HazardEventStatus.ACTIVE);
        event.setDistrict(district);
        event.setOccurredAt(LocalDateTime.now().minusHours(2));
        events.save(event);
        Warning warning = new Warning();
        warning.setHazardEvent(event);
        warning.setSeverity(Severity.HIGH);
        warning.setStatus(WarningStatus.ISSUED);
        warning.setMessage("Gin Ganga flood warning");
        warning.setIssuedBy(officer);
        warning.setIssuedAt(LocalDateTime.now().minusHours(1));
        warnings.save(warning);
        Shelter shelter = new Shelter();
        shelter.setName("Galle Municipal Hall");
        shelter.setDistrict(district);
        shelter.setOrganization(org);
        shelter.setCapacity(100);
        shelter.setCurrentOccupancy(40);
        shelter.setStatus(ShelterStatus.OPEN);
        shelters.save(shelter);

        mvc.perform(post("/post-event-reports").param("hazardEventId", event.getId().toString()))
                .andExpect(status().is3xxRedirection());
        PostEventReport report = reports.findAll().get(0);

        String list = mvc.perform(get("/post-event-reports")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(list).contains("Generate report").contains("#" + report.getId());

        String detail = mvc.perform(get("/post-event-reports/" + report.getId())).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(detail).contains("Gin Ganga flood warning").contains("Galle Municipal Hall (40/100)")
                .contains("Alert timeline");

        // The same pages as JSON, plus the drop-down pages that carry entity lists.
        String json = mvc.perform(get("/post-event-reports/" + report.getId()).accept("application/json"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(json).contains("Gin Ganga flood warning").contains("Galle Municipal Hall (40/100)");
        for (String url : new String[] {"/post-event-reports", "/warnings/new", "/shelters/new", "/rescue-requests/new",
                "/relief-distributions/new", "/relief-supplies/new"}) {
            mvc.perform(get(url).accept("application/json")).andExpect(status().isOk());
        }
    }
}
