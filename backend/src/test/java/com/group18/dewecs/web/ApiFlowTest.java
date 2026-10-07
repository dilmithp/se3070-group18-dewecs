package com.group18.dewecs.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.GroundReportStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInRelativeOrder;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiFlowTest extends ApiTestSupport {

    private District namedDistrict(String name) {
        District d = new District();
        d.setName(name);
        return districtRepository.save(d);
    }

    @Test
    void referenceDataSortsDistrictsIgnoringCaseAndListsTheCategories() throws Exception {
        namedDistrict("banana");
        namedDistrict("Apple");
        namedDistrict("cherry");

        mvc.perform(get("/api/v1/reference-data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.districts[*].name", containsInRelativeOrder("Apple", "banana", "cherry")))
                .andExpect(jsonPath("$.categories[0]").value("FLOOD"))
                .andExpect(jsonPath("$.categories.length()").value(4));
    }

    @Test
    void identifyCreatesThenFindsAndNeverOverwritesTheStoredCitizen() throws Exception {
        District d = district();
        String nic = uniqueNic();

        MvcResult first = identify(d, nic, "  Nimal Perera ");
        assertThat(first.getResponse().getStatus()).isEqualTo(201);
        assertThat(body(first).get("created").asBoolean()).isTrue();
        assertThat(body(first).get("fullName").asText()).isEqualTo("Nimal Perera");
        assertThat(body(first).get("districtName").asText()).isEqualTo("Test District");

        MvcResult second = identify(d, nic.toLowerCase(), "Somebody Else");
        assertThat(second.getResponse().getStatus()).isEqualTo(200);
        assertThat(body(second).get("created").asBoolean()).isFalse();
        assertThat(body(second).get("id").asLong()).isEqualTo(body(first).get("id").asLong());
        assertThat(body(second).get("fullName").asText()).isEqualTo("Nimal Perera");
    }

    @Test
    void wholeFlowSubmitReplayPhotoGetAndList() throws Exception {
        District d = district();
        long citizenId = newCitizenId(d);
        LocalDateTime capturedAt = minutesAgo(10);

        MvcResult created = submit(reportRequest(citizenId, d, capturedAt));
        assertThat(created.getResponse().getStatus()).isEqualTo(201);
        JsonNode report = body(created);
        long reportId = report.get("id").asLong();
        assertThat(created.getResponse().getHeader("Location")).isEqualTo("/api/v1/ground-reports/" + reportId);
        assertThat(report.get("status").asText()).isEqualTo("PENDING_REVIEW");
        assertThat(report.get("photoUrl").isNull()).isTrue();
        assertThat(report.get("actionNote").isNull()).isTrue();
        assertThat(report.get("gpsLat").decimalValue()).isEqualByComparingTo("6.9271234");
        assertThat(report.get("submittedAt").asText()).isEqualTo(
                capturedAt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")));
        assertThat(report.get("citizenId").asLong()).isEqualTo(citizenId);
        assertThat(report.has("reportedBy")).isFalse();
        assertThat(report.has("phone")).isFalse();

        MvcResult replay = submit(reportRequest(citizenId, d, capturedAt));
        assertThat(replay.getResponse().getStatus()).isEqualTo(200);
        assertThat(body(replay).get("id").asLong()).isEqualTo(reportId);
        assertThat(groundReportRepository.count()).isEqualTo(1);

        MvcResult uploaded = upload(reportId, PNG);
        assertThat(uploaded.getResponse().getStatus()).isEqualTo(200);
        String photoUrl = body(uploaded).get("photoUrl").asText();
        assertThat(photoUrl).matches("^/api/v1/photos/[0-9a-f-]{36}\\.png$");

        mvc.perform(get("/api/v1/ground-reports/{id}", reportId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoUrl").value(photoUrl));

        MvcResult photo = mvc.perform(get(photoUrl))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Cache-Control", allOf(containsString("public"), containsString("max-age=86400"))))
                .andReturn();
        assertThat(photo.getResponse().getContentAsByteArray()).isEqualTo(PNG);

        String again = body(upload(reportId, PNG)).get("photoUrl").asText();
        assertThat(again).isNotEqualTo(photoUrl);
        mvc.perform(get(photoUrl)).andExpect(status().isNotFound());

        MvcResult newer = submit(reportRequest(citizenId, d, minutesAgo(1)));
        long newerId = body(newer).get("id").asLong();
        mvc.perform(get("/api/v1/citizens/{id}/ground-reports", citizenId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(newerId))
                .andExpect(jsonPath("$.items[1].id").value(reportId))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void listPagingClampsSizeAndPage() throws Exception {
        District d = district();
        long citizenId = newCitizenId(d);
        for (int i = 1; i <= 3; i++) {
            submit(reportRequest(citizenId, d, minutesAgo(i * 10)));
        }

        mvc.perform(get("/api/v1/citizens/{id}/ground-reports?page=-5&size=2", citizenId))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
        mvc.perform(get("/api/v1/citizens/{id}/ground-reports?page=1&size=2", citizenId))
                .andExpect(jsonPath("$.items.length()").value(1));
        mvc.perform(get("/api/v1/citizens/{id}/ground-reports?size=1000", citizenId))
                .andExpect(jsonPath("$.size").value(50));
        mvc.perform(get("/api/v1/citizens/{id}/ground-reports?size=0", citizenId))
                .andExpect(jsonPath("$.size").value(1));
    }

    @Test
    void actionNoteIsOnlyExposedOnceTheReportIsActioned() throws Exception {
        District d = district();
        long citizenId = newCitizenId(d);
        long reportId = body(submit(reportRequest(citizenId, d, minutesAgo(5)))).get("id").asLong();
        var report = groundReportRepository.findById(reportId).orElseThrow();
        report.setActionNote("internal note");
        report.setStatus(GroundReportStatus.VERIFIED);

        mvc.perform(get("/api/v1/ground-reports/{id}", reportId))
                .andExpect(jsonPath("$.actionNote").value((Object) null));

        report.setStatus(GroundReportStatus.ACTIONED);
        mvc.perform(get("/api/v1/ground-reports/{id}", reportId))
                .andExpect(jsonPath("$.actionNote").value("internal note"));
    }

    @Test
    void corsHeadersAreAbsentByDefault() throws Exception {
        mvc.perform(get("/api/v1/reference-data").header("Origin", "http://localhost:5555"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
