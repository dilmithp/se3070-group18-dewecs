package com.group18.dewecs.web;

import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.HazardEvent;
import com.group18.dewecs.domain.Organization;
import com.group18.dewecs.domain.Shelter;
import com.group18.dewecs.domain.ShelterStatus;
import com.group18.dewecs.domain.User;
import com.group18.dewecs.domain.Warning;
import com.group18.dewecs.repository.WarningRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The officer pages also answer in JSON: pages by Accept header or ?format=json, actions with a JSON body. */
class OfficerJsonTest extends FlowTestSupport {

    private static final String JSON = "application/json";

    @Autowired
    private WarningRepository warningRepository;

    @Test
    void aListPageReturnsItsModelAsJsonWithoutTheFormBean() throws Exception {
        shelter(district(), organization(), 50);

        mvc.perform(get("/shelters").accept(JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.shelters[0].name").value("Test Shelter"))
                .andExpect(jsonPath("$.shelters[0].capacity").value(50))
                .andExpect(jsonPath("$.statuses[0]").value("OPEN"))
                .andExpect(jsonPath("$.form").doesNotExist());
    }

    @Test
    void formatJsonWorksWithoutAnAcceptHeader() throws Exception {
        shelter(district(), organization(), 10);

        mvc.perform(get("/shelters").param("format", "json"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.shelters.length()").value(1));
    }

    @Test
    void aBrowserStillGetsHtml() throws Exception {
        mvc.perform(get("/shelters").accept("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
        mvc.perform(get("/shelters"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }

    @Test
    void dropDownUsersAreReducedToIdAndName() throws Exception {
        District d = district();
        officer(d);

        MvcResult result = mvc.perform(get("/warnings/new").accept(JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users[0].fullName").value("Test Officer"))
                .andExpect(jsonPath("$.users[0].id").exists())
                .andExpect(jsonPath("$.users[0].phone").doesNotExist())
                .andReturn();
        assertThat(result.getResponse().getContentAsString()).doesNotContain("0710000000");
    }

    @Test
    void theDashboardAndADetailPageAreJsonToo() throws Exception {
        Shelter s = shelter(district(), organization(), 5);

        mvc.perform(get("/dashboard").accept(JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.openWarnings").value(0));
        mvc.perform(get("/shelters/" + s.getId()).accept(JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shelter.name").value("Test Shelter"))
                .andExpect(jsonPath("$.occupants.length()").value(0));
    }

    @Test
    void createWithAJsonBodyReturns201WithLocationAndMessage() throws Exception {
        District d = district();
        Organization o = organization();
        String body = "{\"districtId\":" + d.getId() + ",\"organizationId\":" + o.getId()
                + ",\"name\":\"Town Hall\",\"capacity\":30}";

        mvc.perform(post("/shelters").contentType(JSON).accept(JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/shelters/")))
                .andExpect(jsonPath("$.message").value("Shelter created."))
                .andExpect(jsonPath("$.location").value(org.hamcrest.Matchers.startsWith("/shelters/")));

        Shelter created = shelterRepository.findAll().get(0);
        assertThat(created.getName()).isEqualTo("Town Hall");
        assertThat(created.getCapacity()).isEqualTo(30);
        assertThat(created.getStatus()).isEqualTo(ShelterStatus.OPEN);
    }

    @Test
    void missingFieldsGiveA400ProblemWithFieldErrors() throws Exception {
        mvc.perform(post("/shelters").contentType(JSON).accept(JSON).content("{\"capacity\":5}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.name").value("Enter a name"))
                .andExpect(jsonPath("$.fieldErrors.districtId").value("Select a district"));

        assertThat(shelterRepository.count()).isZero();
    }

    @Test
    void aZeroCapacityIsRejectedWithAFieldError() throws Exception {
        District d = district();
        Organization o = organization();
        String body = "{\"districtId\":" + d.getId() + ",\"organizationId\":" + o.getId()
                + ",\"name\":\"Bad\",\"capacity\":0}";

        mvc.perform(post("/shelters").contentType(JSON).accept(JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.fieldErrors.capacity").exists());
        assertThat(shelterRepository.count()).isZero();
    }

    @Test
    void anActionWithNoBodyReturnsAJsonMessageAndARuleViolationIsA400() throws Exception {
        Shelter s = shelter(district(), organization(), 5);

        mvc.perform(post("/shelters/" + s.getId() + "/close").accept(JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Shelter closed."))
                .andExpect(jsonPath("$.location").value("/shelters/" + s.getId()));
        assertThat(s.getStatus()).isEqualTo(ShelterStatus.CLOSED);

        mvc.perform(post("/shelters/" + s.getId() + "/check-in").contentType(JSON).accept(JSON)
                        .content("{\"fullName\":\"Amaya\",\"nic\":\"200000000101\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Cannot check in: shelter is closed."));
    }

    @Test
    void anUnknownRecordIsA404Problem() throws Exception {
        mvc.perform(get("/shelters/999999").accept(JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void aJsonArrayBecomesRepeatedParametersAndTheWarningIsSaved() throws Exception {
        District d = district();
        HazardEvent event = hazardEvent(d);
        User officer = officer(d);
        String body = "{\"hazardEventId\":" + event.getId() + ",\"issuedByUserId\":" + officer.getId()
                + ",\"severity\":\"HIGH\",\"message\":\"Move to higher ground\","
                + "\"broadcastChannels\":[\"SMS\",\"RADIO\"]}";

        mvc.perform(post("/warnings").contentType(JSON).accept(JSON).content(body))
                .andExpect(status().isCreated());

        Warning saved = warningRepository.findAll().get(0);
        assertThat(saved.getMessage()).isEqualTo("Move to higher ground");
        assertThat(saved.getBroadcastChannels()).hasSize(2);
    }

    @Test
    void malformedJsonAndNonObjectBodiesAre400() throws Exception {
        mvc.perform(post("/shelters").contentType(JSON).accept(JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("The request body is not valid JSON."));
        mvc.perform(post("/shelters").contentType(JSON).accept(JSON).content("[1,2]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("The request body must be a JSON object."));
    }

    @Test
    void aBrowserFormPostStillRedirects() throws Exception {
        District d = district();
        Organization o = organization();
        mvc.perform(post("/shelters")
                        .param("districtId", d.getId().toString())
                        .param("organizationId", o.getId().toString())
                        .param("name", "Form Hall")
                        .param("capacity", "4"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void postEventReportsAreJsonToo() throws Exception {
        HazardEvent event = hazardEvent(district());

        MvcResult created = mvc.perform(post("/post-event-reports").contentType(JSON).accept(JSON)
                        .content("{\"hazardEventId\":" + event.getId() + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Post-event report generated."))
                .andReturn();
        String location = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.location");

        mvc.perform(get(location).accept(JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.report.hazardType").value("FLOOD"))
                .andExpect(jsonPath("$.report.metrics.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)));
    }

    @Test
    void everyPageWithEntityDropDownsSerializesAsJson() throws Exception {
        District d = district();
        Organization o = organization();
        shelter(d, o, 20);
        supply(d, o, 100);
        officer(d);
        hazardEvent(d);

        for (String url : new String[] {"/warnings/new", "/shelters/new", "/rescue-requests/new", "/relief-supplies",
                "/relief-supplies/new", "/relief-distributions/new", "/relief-distributions", "/post-event-reports",
                "/ground-reports", "/warnings", "/rescue-requests"}) {
            mvc.perform(get(url).accept(JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }
        mvc.perform(get("/relief-distributions/new").accept(JSON))
                .andExpect(jsonPath("$.supplies[0].name").value("Test Rice"))
                .andExpect(jsonPath("$.shelters[0].name").value("Test Shelter"));
    }

    @Test
    void theMobileApiIsNotAffectedByTheFormatParameter() throws Exception {
        mvc.perform(get("/api/v1/reference-data").param("format", "html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}
