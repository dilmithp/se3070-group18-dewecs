package com.group18.dewecs.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** Timestamps always carry exactly three fraction digits, including when the milliseconds or seconds are zero. */
class ApiTimestampFormatTest extends FlowTestSupport {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void timestampsAreSerialisedWithExactlyThreeFractionDigits() throws Exception {
        assertThat(objectMapper.writeValueAsString(LocalDateTime.parse("2026-10-07T14:03:11.123")))
                .isEqualTo("\"2026-10-07T14:03:11.123\"");
        assertThat(objectMapper.writeValueAsString(LocalDateTime.parse("2026-10-07T14:03:11.440")))
                .isEqualTo("\"2026-10-07T14:03:11.440\"");
        assertThat(objectMapper.writeValueAsString(LocalDateTime.parse("2026-10-07T14:03:11")))
                .isEqualTo("\"2026-10-07T14:03:11.000\"");
        assertThat(objectMapper.writeValueAsString(LocalDateTime.parse("2026-10-07T14:03")))
                .isEqualTo("\"2026-10-07T14:03:00.000\"");
    }

    @Test
    void timestampsAreStillReadLeniently() throws Exception {
        assertThat(objectMapper.readValue("\"2026-10-07T14:03:11.44\"", LocalDateTime.class))
                .isEqualTo(LocalDateTime.parse("2026-10-07T14:03:11.440"));
        assertThat(objectMapper.readValue("\"2026-10-07T14:03\"", LocalDateTime.class))
                .isEqualTo(LocalDateTime.parse("2026-10-07T14:03:00"));
    }
}
