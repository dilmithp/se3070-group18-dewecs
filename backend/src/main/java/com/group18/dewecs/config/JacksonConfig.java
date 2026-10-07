package com.group18.dewecs.config;

import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Mobile API timestamps always have exactly three fraction digits (2026-10-07T14:03:11.440, never ...11.44 or ...11),
 * so every client can parse them the same way. Reading stays lenient ISO-8601.
 */
@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter MILLIS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer localDateTimeAsMillis() {
        return builder -> builder.serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(MILLIS));
    }
}
