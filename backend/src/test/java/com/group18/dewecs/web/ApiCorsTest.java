package com.group18.dewecs.web;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = "dewecs.api.cors.allowed-origin-patterns=http://localhost:*")
class ApiCorsTest extends ApiTestSupport {

    @Test
    void allowedOriginGetsCorsHeadersOnApiPaths() throws Exception {
        mvc.perform(get("/api/v1/reference-data").header("Origin", "http://localhost:5555"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5555"));
    }

    @Test
    void preflightIsAnsweredForAllowedOrigin() throws Exception {
        mvc.perform(options("/api/v1/ground-reports")
                        .header("Origin", "http://localhost:5555")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5555"));
    }

    @Test
    void otherOriginsAreRejected() throws Exception {
        mvc.perform(get("/api/v1/reference-data").header("Origin", "http://evil.example"))
                .andExpect(status().isForbidden());
    }

    @Test
    void webPagesNeverGetCorsHeaders() throws Exception {
        mvc.perform(get("/dashboard").header("Origin", "http://localhost:5555"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
