package com.group18.dewecs.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Makes the Google Maps browser key available to the page templates as the request attribute "mapsApiKey". It is
 * deliberately not a model attribute, so it never appears in the JSON form of an officer page. Without a key the map
 * widgets are not rendered and the forms keep their plain latitude and longitude fields.
 */
@Component
public class MapsKeyFilter extends OncePerRequestFilter {

    private final String key;

    public MapsKeyFilter(@Value("${dewecs.google.maps.api-key:}") String key) {
        this.key = key == null ? "" : key.trim();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!key.isEmpty()) {
            request.setAttribute("mapsApiKey", key);
        }
        chain.doFilter(request, response);
    }
}
