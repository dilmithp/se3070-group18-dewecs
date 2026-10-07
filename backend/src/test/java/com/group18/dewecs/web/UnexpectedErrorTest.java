package com.group18.dewecs.web;

import com.group18.dewecs.service.ShelterService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** A genuinely unexpected failure is a 500 that never leaks the exception text or a stack trace. */
class UnexpectedErrorTest extends FlowTestSupport {

    @MockitoBean
    private ShelterService shelterService;

    @Test
    void unexpectedFailureShowsAGenericPageWith500() throws Exception {
        when(shelterService.list(any(), any())).thenThrow(new IllegalStateException("secret internal detail"));

        mvc.perform(get("/shelters").accept(MediaType.TEXT_HTML))
                .andExpect(status().isInternalServerError())
                .andExpect(view().name("error"))
                .andExpect(content().string(containsString("Something went wrong")))
                .andExpect(content().string(not(containsString("secret internal detail"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }

    @Test
    void unexpectedFailureStaysProblemDetailJsonForNonBrowsers() throws Exception {
        when(shelterService.list(any(), any())).thenThrow(new IllegalStateException("secret internal detail"));

        mvc.perform(get("/shelters").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Unexpected error occurred"));
    }
}
