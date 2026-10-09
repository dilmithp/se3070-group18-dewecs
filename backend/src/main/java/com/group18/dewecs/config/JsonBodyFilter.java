package com.group18.dewecs.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lets an officer-page action accept a JSON request body. The flat JSON object is exposed as ordinary request
 * parameters (arrays become repeated parameters), so the existing form binding and Bean Validation run unchanged.
 * Only POST/PUT/PATCH/DELETE with Content-Type application/json outside /api/** are touched.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class JsonBodyFilter extends OncePerRequestFilter {

    static final int MAX_BYTES = 256 * 1024;

    private final ObjectMapper objectMapper;

    public JsonBodyFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String contentType = request.getContentType();
        return request.getRequestURI().startsWith("/api/")
                || contentType == null
                || !MediaType.APPLICATION_JSON.isCompatibleWith(MediaType.parseMediaType(contentType))
                || "GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        byte[] body = request.getInputStream().readNBytes(MAX_BYTES + 1);
        if (body.length > MAX_BYTES) {
            problem(response, HttpStatus.PAYLOAD_TOO_LARGE, "The JSON body is larger than " + MAX_BYTES / 1024 + " KB.");
            return;
        }
        if (body.length == 0) {
            chain.doFilter(request, response);
            return;
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(body);
        } catch (IOException e) {
            problem(response, HttpStatus.BAD_REQUEST, "The request body is not valid JSON.");
            return;
        }
        if (root == null || !root.isObject()) {
            problem(response, HttpStatus.BAD_REQUEST, "The request body must be a JSON object.");
            return;
        }
        Map<String, String[]> parameters = new LinkedHashMap<>(request.getParameterMap());
        root.fields().forEachRemaining(entry -> {
            JsonNode value = entry.getValue();
            if (value.isNull()) {
                return;
            }
            if (value.isArray()) {
                parameters.put(entry.getKey(), toStrings(value));
            } else if (value.isValueNode()) {
                parameters.put(entry.getKey(), new String[] {value.asText()});
            }
            // nested objects are ignored: the officer forms are flat
        });
        chain.doFilter(new ParameterRequest(request, parameters), response);
    }

    private String[] toStrings(JsonNode array) {
        java.util.List<String> values = new java.util.ArrayList<>();
        array.forEach(item -> {
            if (item.isValueNode() && !item.isNull()) {
                values.add(item.asText());
            }
        });
        return values.toArray(String[]::new);
    }

    private void problem(HttpServletResponse response, HttpStatus status, String detail) throws IOException {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setTitle(status.getReasonPhrase());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    /** The request with its parameters replaced by the merged map. */
    private static final class ParameterRequest extends HttpServletRequestWrapper {

        private final Map<String, String[]> parameters;

        ParameterRequest(HttpServletRequest request, Map<String, String[]> parameters) {
            super(request);
            this.parameters = parameters;
        }

        @Override
        public String getParameter(String name) {
            String[] values = parameters.get(name);
            return values == null || values.length == 0 ? null : values[0];
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            return Collections.unmodifiableMap(parameters);
        }

        @Override
        public Enumeration<String> getParameterNames() {
            return Collections.enumeration(parameters.keySet());
        }

        @Override
        public String[] getParameterValues(String name) {
            return parameters.get(name);
        }
    }
}
