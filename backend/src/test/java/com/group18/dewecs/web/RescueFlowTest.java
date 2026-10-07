package com.group18.dewecs.web;

import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.Organization;
import com.group18.dewecs.domain.RescueRequest;
import com.group18.dewecs.domain.RescueRequestStatus;
import com.group18.dewecs.domain.RescueTeam;
import com.group18.dewecs.domain.RescueTeamStatus;
import com.group18.dewecs.domain.Severity;
import com.group18.dewecs.repository.RescueRequestRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class RescueFlowTest extends FlowTestSupport {

    @Autowired
    private RescueRequestRepository requestRepository;

    private RescueRequest submit(District d, String priority) throws Exception {
        mvc.perform(post("/rescue-requests")
                        .param("districtId", d.getId().toString())
                        .param("requesterName", "Kamal")
                        .param("requesterPhone", "0771234567")
                        .param("description", "Stranded")
                        .param("priority", priority))
                .andExpect(status().is3xxRedirection());
        return requestRepository.findAll().get(0);
    }

    @Test
    void submitAssignAndCompleteMovesTheTeamThroughDispatchedAndBack() throws Exception {
        District d = district();
        Organization o = organization();
        RescueTeam team = team(d, o, RescueTeamStatus.AVAILABLE);

        RescueRequest request = submit(d, "CRITICAL");
        assertThat(request.getStatus()).isEqualTo(RescueRequestStatus.PENDING);
        assertThat(request.getPriority()).isEqualTo(Severity.CRITICAL);

        mvc.perform(post("/rescue-requests/" + request.getId() + "/assign").param("teamId", team.getId().toString()))
                .andExpect(status().is3xxRedirection());
        assertThat(request.getStatus()).isEqualTo(RescueRequestStatus.ASSIGNED);
        assertThat(team.getStatus()).isEqualTo(RescueTeamStatus.DISPATCHED);

        mvc.perform(post("/rescue-requests/" + request.getId() + "/complete"))
                .andExpect(status().is3xxRedirection());
        assertThat(request.getStatus()).isEqualTo(RescueRequestStatus.COMPLETED);
        assertThat(team.getStatus()).isEqualTo(RescueTeamStatus.AVAILABLE);
    }

    @Test
    void cancellingAnAssignedRequestMakesTheTeamAvailableAgain() throws Exception {
        District d = district();
        RescueTeam team = team(d, organization(), RescueTeamStatus.AVAILABLE);
        RescueRequest request = submit(d, "HIGH");
        mvc.perform(post("/rescue-requests/" + request.getId() + "/assign").param("teamId", team.getId().toString()));
        assertThat(team.getStatus()).isEqualTo(RescueTeamStatus.DISPATCHED);

        mvc.perform(post("/rescue-requests/" + request.getId() + "/cancel"))
                .andExpect(status().is3xxRedirection());
        assertThat(request.getStatus()).isEqualTo(RescueRequestStatus.CANCELLED);
        assertThat(team.getStatus()).isEqualTo(RescueTeamStatus.AVAILABLE);
    }

    @Test
    void dispatchedTeamCannotBeAssignedToASecondRequest() throws Exception {
        District d = district();
        RescueTeam team = team(d, organization(), RescueTeamStatus.DISPATCHED);
        RescueRequest request = submit(d, "LOW");

        mvc.perform(post("/rescue-requests/" + request.getId() + "/assign").param("teamId", team.getId().toString()))
                .andExpect(flash().attribute("error", "Team is not available for assignment."));
        assertThat(request.getStatus()).isEqualTo(RescueRequestStatus.PENDING);
    }

    @Test
    void completedRequestCannotBeCancelled() throws Exception {
        District d = district();
        RescueTeam team = team(d, organization(), RescueTeamStatus.AVAILABLE);
        RescueRequest request = submit(d, "LOW");
        mvc.perform(post("/rescue-requests/" + request.getId() + "/assign").param("teamId", team.getId().toString()));
        mvc.perform(post("/rescue-requests/" + request.getId() + "/complete"));

        mvc.perform(post("/rescue-requests/" + request.getId() + "/cancel"))
                .andExpect(flash().attribute("error", "Only pending or assigned requests can be cancelled."));
        assertThat(request.getStatus()).isEqualTo(RescueRequestStatus.COMPLETED);
    }

    @Test
    void submitWithoutDescriptionReShowsTheForm() throws Exception {
        District d = district();
        mvc.perform(post("/rescue-requests")
                        .param("districtId", d.getId().toString())
                        .param("requesterName", "Kamal")
                        .param("requesterPhone", "0771234567")
                        .param("priority", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(view().name("rescue-requests/form"));
        assertThat(requestRepository.count()).isZero();
    }

    @Test
    void outOfRangeCoordinatesAreAFormErrorNotADatabaseError() throws Exception {
        District d = district();
        mvc.perform(post("/rescue-requests")
                        .param("districtId", d.getId().toString())
                        .param("requesterName", "Kamal")
                        .param("requesterPhone", "0771234567")
                        .param("description", "Stranded")
                        .param("priority", "HIGH")
                        .param("gpsLat", "1000")
                        .param("gpsLng", "-181"))
                .andExpect(status().isOk())
                .andExpect(view().name("rescue-requests/form"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.model()
                        .attributeHasFieldErrors("form", "gpsLat", "gpsLng"));
        assertThat(requestRepository.count()).isZero();
    }

    @Test
    void validCoordinatesAreAccepted() throws Exception {
        District d = district();
        mvc.perform(post("/rescue-requests")
                        .param("districtId", d.getId().toString())
                        .param("requesterName", "Kamal")
                        .param("requesterPhone", "0771234567")
                        .param("description", "Stranded")
                        .param("priority", "HIGH")
                        .param("gpsLat", "6.9271234")
                        .param("gpsLng", "79.8612345"))
                .andExpect(status().is3xxRedirection());
        assertThat(requestRepository.findAll().get(0).getGpsLat()).isEqualByComparingTo("6.9271234");
    }
}
