package com.group18.dewecs.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * Optional CORS for /api/** only, for a Flutter web build during development. Off unless
 * dewecs.api.cors.allowed-origin-patterns is set (comma separated; the local profile sets localhost patterns).
 */
@Configuration
public class ApiCorsConfig implements WebMvcConfigurer {

    private final String[] allowedOriginPatterns;

    public ApiCorsConfig(@Value("${dewecs.api.cors.allowed-origin-patterns:}") String patterns) {
        this.allowedOriginPatterns = Arrays.stream(patterns.split(","))
                .map(String::trim).filter(p -> !p.isEmpty()).toArray(String[]::new);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (allowedOriginPatterns.length == 0) {
            return;
        }
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOriginPatterns)
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*");
    }
}
