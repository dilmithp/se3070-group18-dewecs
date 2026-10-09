package com.group18.dewecs.web;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** CORS can still be switched off: an empty dewecs.api.cors.allowed-origin-patterns sends no CORS headers at all. */
@TestPropertySource(properties = "dewecs.api.cors.allowed-origin-patterns=")
class ApiCorsOffTest extends ApiTestSupport {

    @Test
    void corsHeadersAreAbsentWhenSwitchedOff() throws Exception {
        mvc.perform(get("/api/v1/reference-data").header("Origin", "http://localhost:5555"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void aBrowserPreflightIsRefusedWhenSwitchedOff() throws Exception {
        mvc.perform(options("/api/v1/citizens/identify")
                        .header("Origin", "http://localhost:5555")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isForbidden());
    }
}
