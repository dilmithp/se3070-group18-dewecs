package com.group18.dewecs.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registers the JSON answer for the officer pages. The mobile API (/api/**) is excluded: its contract is frozen. */
@Configuration
public class OfficerJsonConfig implements WebMvcConfigurer {

    private final ObjectMapper objectMapper;

    public OfficerJsonConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new JsonResponseInterceptor(objectMapper))
                .addPathPatterns("/**")
                .excludePathPatterns("/api/**", "/css/**", "/js/**", "/error");
    }
}
