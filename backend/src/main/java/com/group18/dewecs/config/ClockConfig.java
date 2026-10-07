package com.group18.dewecs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Sri Lanka time (UTC+05:30, no daylight saving). Used by the mobile-API submission service so tests can fix "now".
 * The older services still read LocalDateTime.now() in the JVM's own zone.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Colombo"));
    }
}
