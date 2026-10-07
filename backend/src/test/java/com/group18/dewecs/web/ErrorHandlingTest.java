package com.group18.dewecs.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** Browsers get the friendly error page with the right status; other clients keep ProblemDetail JSON. */
class ErrorHandlingTest extends FlowTestSupport {

    @Test
    void missingRecordShowsTheErrorPageWith404() throws Exception {
        mvc.perform(get("/shelters/999").accept(MediaType.TEXT_HTML))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(content().string(containsString("Shelter not found: 999")))
                .andExpect(content().string(containsString("Back to the dashboard")))
                .andExpect(content().string(not(containsString("Exception"))));
    }

    @Test
    void badPathVariableIs400NotAServerError() throws Exception {
        mvc.perform(get("/shelters/abc").accept(MediaType.TEXT_HTML))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(content().string(containsString("Invalid value for &#39;id&#39;.")));
    }

    @Test
    void missingRequestParameterIs400() throws Exception {
        mvc.perform(post("/rescue-requests/1/assign").accept(MediaType.TEXT_HTML))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(content().string(containsString("teamId")));
    }

    @Test
    void unknownUrlIs404() throws Exception {
        mvc.perform(get("/no-such-page").accept(MediaType.TEXT_HTML))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"));
    }

    @Test
    void wrongHttpMethodIs405() throws Exception {
        mvc.perform(post("/dashboard").accept(MediaType.TEXT_HTML))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(view().name("error"));
    }

    @Test
    void nonBrowserClientsStillGetProblemDetailJson() throws Exception {
        mvc.perform(get("/shelters/999").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Shelter not found: 999"));
        mvc.perform(get("/shelters/abc").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Invalid value for 'id'."));
        mvc.perform(get("/no-such-page").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
        mvc.perform(post("/dashboard").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isMethodNotAllowed());
    }
}
