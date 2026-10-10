package com.group18.dewecs.web;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The shipped default (no property set in the test): any origin may call the mobile API from a browser, so a Flutter
 * web build or any web client works against the deployed server. The officer pages stay closed to other origins.
 */
class ApiCorsDefaultTest extends ApiTestSupport {

    @Test
    void anyOriginGetsCorsHeadersOnApiPaths() throws Exception {
        for (String origin : new String[] {"http://localhost:5555", "https://app.example.com", "http://192.168.1.20:3000"}) {
            mvc.perform(get("/api/v1/reference-data").header("Origin", origin))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", origin))
                    .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
        }
    }

    @Test
    void preflightIsAnsweredForAnyOrigin() throws Exception {
        mvc.perform(options("/api/v1/citizens/identify")
                        .header("Origin", "https://app.example.com")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://app.example.com"));
    }

    @Test
    void officerPagesStillGetNoCorsHeaders() throws Exception {
        mvc.perform(get("/dashboard").header("Origin", "https://app.example.com"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        mvc.perform(get("/shelters").param("format", "json").header("Origin", "https://app.example.com"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
