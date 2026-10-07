package com.group18.dewecs.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group18.dewecs.domain.District;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Helpers for the mobile-API tests. Every test builds its own data with a unique NIC (tests roll back). */
abstract class ApiTestSupport extends FlowTestSupport {

    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3, 4};

    private static final AtomicLong NIC_COUNTER = new AtomicLong(System.nanoTime() % 1_000_000_000L);

    @Autowired
    protected ObjectMapper objectMapper;

    protected String uniqueNic() {
        return String.format("19%010d", NIC_COUNTER.incrementAndGet());
    }

    protected LocalDateTime minutesAgo(int minutes) {
        return LocalDateTime.now(ZoneId.of("Asia/Colombo")).minusMinutes(minutes).truncatedTo(ChronoUnit.MILLIS);
    }

    protected JsonNode body(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    protected MvcResult identify(District district, String nic, String name) throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("nic", nic);
        request.put("fullName", name);
        request.put("phone", "0771234567");
        request.put("districtId", district.getId());
        return mvc.perform(post("/api/v1/citizens/identify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request))).andReturn();
    }

    protected long newCitizenId(District district) throws Exception {
        return body(identify(district, uniqueNic(), "Test Citizen")).get("id").asLong();
    }

    protected Map<String, Object> reportRequest(long citizenId, District district, LocalDateTime capturedAt) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("citizenId", citizenId);
        request.put("districtId", district.getId());
        request.put("category", "FLOOD");
        request.put("description", "River is overflowing near the bridge");
        request.put("gpsLat", 6.9271234);
        request.put("gpsLng", 79.8612345);
        if (capturedAt != null) {
            request.put("capturedAt", capturedAt.toString());
        }
        return request;
    }

    protected MvcResult submit(Map<String, Object> request) throws Exception {
        return mvc.perform(post("/api/v1/ground-reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request))).andReturn();
    }

    protected MvcResult upload(long reportId, byte[] bytes) throws Exception {
        return mvc.perform(multipart("/api/v1/ground-reports/{id}/photo", reportId)
                .file(new MockMultipartFile("file", "whatever.jpg", "image/jpeg", bytes))).andReturn();
    }

}
