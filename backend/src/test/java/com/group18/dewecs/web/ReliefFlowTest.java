package com.group18.dewecs.web;

import com.group18.dewecs.domain.ConsignmentStatus;
import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.Organization;
import com.group18.dewecs.domain.ReliefConsignment;
import com.group18.dewecs.domain.Resource;
import com.group18.dewecs.domain.Shelter;
import com.group18.dewecs.repository.ReliefConsignmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class ReliefFlowTest extends FlowTestSupport {

    @Autowired
    private ReliefConsignmentRepository consignmentRepository;

    private ReliefConsignment distribute(Resource supply, Shelter shelter, int quantity) throws Exception {
        mvc.perform(post("/relief-distributions")
                        .param("resourceId", supply.getId().toString())
                        .param("shelterId", shelter.getId().toString())
                        .param("quantity", String.valueOf(quantity)))
                .andExpect(status().is3xxRedirection());
        var all = consignmentRepository.findAll();
        return all.get(all.size() - 1);
    }

    @Test
    void createSupplyThenRestockIncreasesStock() throws Exception {
        District d = district();
        Organization o = organization();
        mvc.perform(post("/relief-supplies")
                        .param("districtId", d.getId().toString())
                        .param("organizationId", o.getId().toString())
                        .param("name", "Rice")
                        .param("type", "FOOD")
                        .param("unit", "kg")
                        .param("quantity", "100"))
                .andExpect(status().is3xxRedirection());
        Resource supply = resourceRepository.findAll().get(0);
        assertThat(supply.getQuantity()).isEqualTo(100);

        mvc.perform(post("/relief-supplies/" + supply.getId() + "/restock").param("quantity", "25"))
                .andExpect(status().is3xxRedirection());
        assertThat(supply.getQuantity()).isEqualTo(125);

        mvc.perform(post("/relief-supplies/" + supply.getId() + "/restock").param("quantity", "0"))
                .andExpect(status().is3xxRedirection());
        assertThat(supply.getQuantity()).isEqualTo(125);
    }

    @Test
    void distributionDeductsStockCancelRestoresItAndDeliveredCannotBeCancelled() throws Exception {
        District d = district();
        Organization o = organization();
        Resource supply = supply(d, o, 100);
        Shelter shelter = shelter(d, o, 50);

        ReliefConsignment c = distribute(supply, shelter, 40);
        assertThat(supply.getQuantity()).isEqualTo(60);
        assertThat(c.getStatus()).isEqualTo(ConsignmentStatus.DISPATCHED);

        mvc.perform(post("/relief-distributions/" + c.getId() + "/cancel")).andExpect(status().is3xxRedirection());
        assertThat(c.getStatus()).isEqualTo(ConsignmentStatus.CANCELLED);
        assertThat(supply.getQuantity()).isEqualTo(100);

        ReliefConsignment second = distribute(supply, shelter, 10);
        mvc.perform(post("/relief-distributions/" + second.getId() + "/deliver")).andExpect(status().is3xxRedirection());
        assertThat(second.getStatus()).isEqualTo(ConsignmentStatus.DELIVERED);
        assertThat(second.getDeliveredAt()).isNotNull();

        mvc.perform(post("/relief-distributions/" + second.getId() + "/cancel"))
                .andExpect(flash().attribute("error",
                        "Only dispatched (not yet delivered) distributions can be cancelled."));
        assertThat(supply.getQuantity()).isEqualTo(90);
    }

    @Test
    void distributingMoreThanTheStockIsRejected() throws Exception {
        District d = district();
        Organization o = organization();
        Resource supply = supply(d, o, 5);
        Shelter shelter = shelter(d, o, 50);

        mvc.perform(post("/relief-distributions")
                        .param("resourceId", supply.getId().toString())
                        .param("shelterId", shelter.getId().toString())
                        .param("quantity", "6"))
                .andExpect(status().isOk())
                .andExpect(view().name("relief-distributions/form"));
        assertThat(supply.getQuantity()).isEqualTo(5);
        assertThat(consignmentRepository.count()).isZero();
    }
}
