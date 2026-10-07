package com.group18.dewecs.web;

import com.group18.dewecs.domain.BroadcastChannel;
import com.group18.dewecs.domain.ConsignmentItem;
import com.group18.dewecs.domain.ConsignmentStatus;
import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.GroundReport;
import com.group18.dewecs.domain.GroundReportStatus;
import com.group18.dewecs.domain.HazardEvent;
import com.group18.dewecs.domain.Organization;
import com.group18.dewecs.domain.ReliefConsignment;
import com.group18.dewecs.domain.RescueRequest;
import com.group18.dewecs.domain.RescueRequestStatus;
import com.group18.dewecs.domain.RescueTeamStatus;
import com.group18.dewecs.domain.Resource;
import com.group18.dewecs.domain.Severity;
import com.group18.dewecs.domain.Shelter;
import com.group18.dewecs.domain.User;
import com.group18.dewecs.domain.Warning;
import com.group18.dewecs.domain.WarningStatus;
import com.group18.dewecs.repository.ConsignmentItemRepository;
import com.group18.dewecs.repository.ReliefConsignmentRepository;
import com.group18.dewecs.repository.RescueRequestRepository;
import com.group18.dewecs.repository.WarningRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Every list / new / detail / edit page must render (HTTP 200, HTML, no template error). */
class PageRenderingTest extends FlowTestSupport {

    @Autowired
    private WarningRepository warningRepository;
    @Autowired
    private RescueRequestRepository rescueRequestRepository;
    @Autowired
    private ReliefConsignmentRepository consignmentRepository;
    @Autowired
    private ConsignmentItemRepository itemRepository;

    private void assertRenders(String url) throws Exception {
        MvcResult result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
        assertThat(result.getResponse().getContentType()).startsWith("text/html");
        assertThat(result.getResponse().getContentAsString())
                .as(url).contains("DEWECS").doesNotContain("Whitelabel").doesNotContain("Exception");
    }

    @Test
    void everyPageRendersWithDataPresent() throws Exception {
        District d = district();
        Organization o = organization();
        User officer = officer(d);
        HazardEvent event = hazardEvent(d);
        GroundReport report = groundReport(d, GroundReportStatus.VERIFIED);
        Shelter shelter = shelter(d, o, 5);
        team(d, o, RescueTeamStatus.AVAILABLE);
        Resource supply = supply(d, o, 3);

        Warning w = new Warning();
        w.setHazardEvent(event);
        w.setSeverity(Severity.HIGH);
        w.setStatus(WarningStatus.DRAFT);
        w.setMessage("m");
        w.setIssuedBy(officer);
        w.setExpiresAt(LocalDateTime.now().plusDays(1));
        w.setBroadcastChannels(Set.of(BroadcastChannel.SMS));
        warningRepository.save(w);

        RescueRequest rr = new RescueRequest();
        rr.setDistrict(d);
        rr.setRequesterName("K");
        rr.setRequesterPhone("07");
        rr.setDescription("help");
        rr.setPriority(Severity.HIGH);
        rr.setStatus(RescueRequestStatus.PENDING);
        rr.setSubmittedAt(LocalDateTime.now());
        rescueRequestRepository.save(rr);

        ReliefConsignment c = new ReliefConsignment();
        c.setOrganization(o);
        c.setShelter(shelter);
        c.setStatus(ConsignmentStatus.DISPATCHED);
        c.setDispatchedAt(LocalDateTime.now());
        c = consignmentRepository.save(c);
        ConsignmentItem item = new ConsignmentItem();
        item.setConsignment(c);
        item.setResource(supply);
        item.setQuantity(1);
        c.getItems().add(itemRepository.save(item));

        for (String url : new String[] {
                "/", "/dashboard",
                "/warnings", "/warnings/new", "/warnings/" + w.getId(), "/warnings/" + w.getId() + "/edit",
                "/shelters", "/shelters/new", "/shelters/" + shelter.getId(), "/shelters/" + shelter.getId() + "/edit",
                "/rescue-requests", "/rescue-requests/new", "/rescue-requests/" + rr.getId(),
                "/relief-supplies", "/relief-supplies/new", "/relief-supplies/" + supply.getId(),
                "/relief-supplies/" + supply.getId() + "/edit",
                "/relief-distributions", "/relief-distributions/new", "/relief-distributions/" + c.getId(),
                "/ground-reports", "/ground-reports/" + report.getId()}) {
            assertRenders(url);
        }
    }

    @Test
    void listPagesRenderWhenEmptyAndWithFilters() throws Exception {
        for (String url : new String[] {
                "/warnings", "/shelters", "/rescue-requests", "/relief-supplies", "/relief-distributions",
                "/ground-reports", "/warnings?status=ISSUED&districtId=1", "/shelters?status=FULL",
                "/rescue-requests?status=PENDING&priority=HIGH", "/relief-supplies?lowStockOnly=true",
                "/relief-distributions?status=DELIVERED", "/ground-reports?status=VERIFIED&category=FLOOD",
                "/ground-reports?status=nonsense"}) {
            assertRenders(url);
        }
    }
}
