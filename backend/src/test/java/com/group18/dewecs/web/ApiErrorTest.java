package com.group18.dewecs.web;

import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.GroundReportStatus;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Every API failure is a ProblemDetail JSON with status, title and detail (fieldErrors for body validation). */
@Import(ApiErrorTest.ThrowingController.class)
class ApiErrorTest extends ApiTestSupport {

    /** Test-only endpoints that raise errors a real request cannot reach through MockMvc. */
    @RestController
    static class ThrowingController {

        @GetMapping("/api/v1/test/too-large")
        String tooLarge() {
            throw new MaxUploadSizeExceededException(6L * 1024 * 1024);
        }

        @GetMapping("/api/v1/test/boom")
        String boom() {
            throw new IllegalStateException("secret internal detail");
        }
    }

    private void assertProblem(org.springframework.test.web.servlet.ResultActions actions, int status, String title)
            throws Exception {
        actions.andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    @Test
    void bodyValidationIs400WithFieldErrors() throws Exception {
        assertProblem(mvc.perform(post("/api/v1/ground-reports").contentType(MediaType.APPLICATION_JSON).content("{}")),
                400, "Bad Request");
        mvc.perform(post("/api/v1/ground-reports").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(jsonPath("$.fieldErrors.citizenId").exists())
                .andExpect(jsonPath("$.fieldErrors.description").exists())
                .andExpect(jsonPath("$.fieldErrors.gpsLat").exists());
    }

    @Test
    void outOfRangeCoordinateIsAFieldError() throws Exception {
        District d = district();
        var request = reportRequest(newCitizenId(d), d, null);
        request.put("gpsLat", 91);

        mvc.perform(post("/api/v1/ground-reports").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.gpsLat").exists());
    }

    @Test
    void serviceRuleViolationsAre400WithoutFieldErrors() throws Exception {
        District d = district();
        assertProblem(identify0(d, "12345"), 400, "Bad Request");
        identify0(d, "12345").andExpect(jsonPath("$.fieldErrors").doesNotExist());

        var request = reportRequest(newCitizenId(d), d, null);
        request.put("category", "EARTHQUAKE");
        mvc.perform(post("/api/v1/ground-reports").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());

        var future = reportRequest(newCitizenId(d), d, minutesAgo(-30));
        mvc.perform(post("/api/v1/ground-reports").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(future)))
                .andExpect(status().isBadRequest());
    }

    private org.springframework.test.web.servlet.ResultActions identify0(District d, String nic) throws Exception {
        String json = "{\"nic\":\"" + nic + "\",\"fullName\":\"A\",\"phone\":\"0771234567\",\"districtId\":"
                + d.getId() + "}";
        return mvc.perform(post("/api/v1/citizens/identify").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void unknownRecordsAre404() throws Exception {
        District d = district();
        long citizenId = newCitizenId(d);
        assertProblem(mvc.perform(get("/api/v1/ground-reports/999999999")), 404, "Not Found");
        assertProblem(mvc.perform(get("/api/v1/citizens/999999999/ground-reports")), 404, "Not Found");
        assertProblem(mvc.perform(multipart("/api/v1/ground-reports/999999999/photo")
                .file(new MockMultipartFile("file", "a.png", "image/png", PNG))), 404, "Not Found");

        var unknownCitizen = reportRequest(999999999L, d, null);
        assertProblem(mvc.perform(post("/api/v1/ground-reports").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(unknownCitizen))), 404, "Not Found");

        var unknownDistrict = reportRequest(citizenId, d, null);
        unknownDistrict.put("districtId", 999999999L);
        assertProblem(mvc.perform(post("/api/v1/ground-reports").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(unknownDistrict))), 404, "Not Found");

        String json = "{\"nic\":\"" + uniqueNic() + "\",\"fullName\":\"A\",\"phone\":\"0771234567\",\"districtId\":999999999}";
        assertProblem(mvc.perform(post("/api/v1/citizens/identify").contentType(MediaType.APPLICATION_JSON)
                .content(json)), 404, "Not Found");
    }

    @Test
    void wrongMethodIs405() throws Exception {
        assertProblem(mvc.perform(delete("/api/v1/ground-reports/1")), 405, "Method Not Allowed");
        assertProblem(mvc.perform(post("/api/v1/reference-data")), 405, "Method Not Allowed");
    }

    @Test
    void wrongContentTypeIs415() throws Exception {
        assertProblem(mvc.perform(post("/api/v1/citizens/identify").contentType(MediaType.TEXT_PLAIN).content("x")),
                415, "Unsupported Media Type");
        assertProblem(mvc.perform(post("/api/v1/ground-reports/1/photo")
                .contentType(MediaType.APPLICATION_JSON).content("{}")), 415, "Unsupported Media Type");
    }

    @Test
    void malformedOrMissingJsonBodyIs400() throws Exception {
        assertProblem(mvc.perform(post("/api/v1/citizens/identify").contentType(MediaType.APPLICATION_JSON)
                .content("{")), 400, "Bad Request");
        assertProblem(mvc.perform(post("/api/v1/citizens/identify").contentType(MediaType.APPLICATION_JSON)),
                400, "Bad Request");

        District d = district();
        var request = reportRequest(newCitizenId(d), d, null);
        request.put("capturedAt", "yesterday");
        assertProblem(mvc.perform(post("/api/v1/ground-reports").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request))), 400, "Bad Request");
    }

    @Test
    void wrongTypedParameterAndMissingFilePartAre400() throws Exception {
        assertProblem(mvc.perform(get("/api/v1/ground-reports/abc")), 400, "Bad Request");
        assertProblem(mvc.perform(get("/api/v1/citizens/1/ground-reports?page=abc")), 400, "Bad Request");
        assertProblem(mvc.perform(multipart("/api/v1/ground-reports/1/photo")), 400, "Bad Request");
    }

    @Test
    void unknownApiUrlIs404Json_evenWhenABrowserAsksForHtml() throws Exception {
        assertProblem(mvc.perform(get("/api/v1/nope")), 404, "Not Found");
        assertProblem(mvc.perform(get("/api/v1/nope").accept(MediaType.TEXT_HTML)), 404, "Not Found");
        assertProblem(mvc.perform(get("/api/v1/ground-reports/999999999").accept(MediaType.TEXT_HTML)),
                404, "Not Found");
    }

    @Test
    void photoRulesAre400() throws Exception {
        District d = district();
        long reportId = body(submit(reportRequest(newCitizenId(d), d, null))).get("id").asLong();

        byte[] text = "not an image".getBytes(StandardCharsets.UTF_8);
        assertProblem(mvc.perform(multipart("/api/v1/ground-reports/{id}/photo", reportId)
                .file(new MockMultipartFile("file", "fake.jpg", "image/jpeg", text))), 400, "Bad Request");
        assertProblem(mvc.perform(multipart("/api/v1/ground-reports/{id}/photo", reportId)
                .file(new MockMultipartFile("file", "empty.png", "image/png", new byte[0]))), 400, "Bad Request");
        byte[] big = new byte[5 * 1024 * 1024 + 1];
        System.arraycopy(PNG, 0, big, 0, PNG.length);
        assertProblem(mvc.perform(multipart("/api/v1/ground-reports/{id}/photo", reportId)
                .file(new MockMultipartFile("file", "big.png", "image/png", big))), 400, "Bad Request");

        groundReportRepository.findById(reportId).orElseThrow().setStatus(GroundReportStatus.VERIFIED);
        assertProblem(mvc.perform(multipart("/api/v1/ground-reports/{id}/photo", reportId)
                .file(new MockMultipartFile("file", "late.png", "image/png", PNG))), 400, "Bad Request");
    }

    @Test
    void photoDownloadOnlyAcceptsGeneratedNames() throws Exception {
        assertProblem(mvc.perform(get("/api/v1/photos/evil.txt")), 404, "Not Found");
        assertProblem(mvc.perform(get("/api/v1/photos/123e4567-e89b-12d3-a456-426614174000.png")), 404, "Not Found");
        assertProblem(mvc.perform(get("/api/v1/photos/secret.png")), 404, "Not Found");
    }

    @Test
    void uploadOverTheContainerLimitIs413ThroughTheHandler() throws Exception {
        assertProblem(mvc.perform(get("/api/v1/test/too-large")), 413, "Payload Too Large");
    }

    @Test
    void unexpectedErrorIs500WithNoInternalDetail() throws Exception {
        assertProblem(mvc.perform(get("/api/v1/test/boom")), 500, "Internal Server Error");
        mvc.perform(get("/api/v1/test/boom"))
                .andExpect(jsonPath("$.detail").value("Unexpected error occurred"))
                .andExpect(content().string(not(containsString("secret internal detail"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }
}
