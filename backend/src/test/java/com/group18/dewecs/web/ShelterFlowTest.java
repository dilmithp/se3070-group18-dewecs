package com.group18.dewecs.web;

import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.Organization;
import com.group18.dewecs.domain.Shelter;
import com.group18.dewecs.domain.ShelterStatus;
import com.group18.dewecs.repository.ShelterOccupantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class ShelterFlowTest extends FlowTestSupport {

    @Autowired
    private ShelterOccupantRepository occupantRepository;

    private void checkIn(Shelter shelter, String name, String nic) throws Exception {
        mvc.perform(post("/shelters/" + shelter.getId() + "/check-in")
                .param("fullName", name).param("nic", nic));
    }

    @Test
    void createdShelterStartsOpenAndEmpty() throws Exception {
        District d = district();
        Organization o = organization();
        mvc.perform(post("/shelters")
                        .param("districtId", d.getId().toString())
                        .param("organizationId", o.getId().toString())
                        .param("name", "Town Hall")
                        .param("capacity", "3"))
                .andExpect(status().is3xxRedirection());
        Shelter created = shelterRepository.findAll().get(0);
        assertThat(created.getStatus()).isEqualTo(ShelterStatus.OPEN);
        assertThat(created.getCurrentOccupancy()).isZero();
    }

    @Test
    void zeroCapacityIsRejectedAndTheFormIsReShown() throws Exception {
        District d = district();
        Organization o = organization();
        mvc.perform(post("/shelters")
                        .param("districtId", d.getId().toString())
                        .param("organizationId", o.getId().toString())
                        .param("name", "Bad")
                        .param("capacity", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("shelters/form"));
        assertThat(shelterRepository.count()).isZero();
    }

    @Test
    void checkingInUntilCapacityMakesItFullAndCheckOutReopensIt() throws Exception {
        Shelter s = shelter(district(), organization(), 2);
        checkIn(s, "A", "111");
        assertThat(s.getStatus()).isEqualTo(ShelterStatus.OPEN);
        checkIn(s, "B", "222");
        assertThat(s.getStatus()).isEqualTo(ShelterStatus.FULL);
        assertThat(s.getCurrentOccupancy()).isEqualTo(2);

        mvc.perform(post("/shelters/" + s.getId() + "/check-in").param("fullName", "C").param("nic", "333"))
                .andExpect(flash().attribute("error", "Cannot check in: shelter is at full capacity."));
        assertThat(s.getCurrentOccupancy()).isEqualTo(2);

        Long occupantId = occupantRepository.findByShelter_IdAndCheckOutTimeIsNull(s.getId()).get(0).getId();
        mvc.perform(post("/shelters/" + s.getId() + "/check-out/" + occupantId))
                .andExpect(flash().attribute("message", "Occupant checked out."));
        assertThat(s.getStatus()).isEqualTo(ShelterStatus.OPEN);
        assertThat(s.getCurrentOccupancy()).isEqualTo(1);

        mvc.perform(post("/shelters/" + s.getId() + "/check-out/" + occupantId))
                .andExpect(flash().attribute("error", "Occupant has already checked out."));
        assertThat(s.getCurrentOccupancy()).isEqualTo(1);
    }

    @Test
    void closedShelterRejectsCheckInAndReopensToTheRightStatus() throws Exception {
        Shelter s = shelter(district(), organization(), 5);
        mvc.perform(post("/shelters/" + s.getId() + "/close")).andExpect(status().is3xxRedirection());
        assertThat(s.getStatus()).isEqualTo(ShelterStatus.CLOSED);

        mvc.perform(post("/shelters/" + s.getId() + "/check-in").param("fullName", "A").param("nic", "111"))
                .andExpect(flash().attribute("error", "Cannot check in: shelter is closed."));

        mvc.perform(post("/shelters/" + s.getId() + "/reopen")).andExpect(status().is3xxRedirection());
        assertThat(s.getStatus()).isEqualTo(ShelterStatus.OPEN);

        mvc.perform(post("/shelters/" + s.getId() + "/reopen"))
                .andExpect(flash().attribute("error", "Only a closed shelter can be reopened."));
    }

    @Test
    void checkInWithoutNicIsRejectedByValidation() throws Exception {
        Shelter s = shelter(district(), organization(), 5);
        mvc.perform(post("/shelters/" + s.getId() + "/check-in").param("fullName", "A"))
                .andExpect(flash().attribute("error", "Enter both a name and NIC to check in an occupant."));
        assertThat(s.getCurrentOccupancy()).isZero();
    }
}
