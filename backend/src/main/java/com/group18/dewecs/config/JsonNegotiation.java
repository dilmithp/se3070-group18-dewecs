package com.group18.dewecs.config;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The officer pages (everything except /api/**) answer with HTML by default and with JSON when the client asks for it:
 * either {@code Accept: application/json} (without text/html, which browsers always send) or {@code ?format=json}.
 */
public final class JsonNegotiation {

    private JsonNegotiation() {
    }

    /** True for the officer pages when the client wants JSON. Never true for /api/** (the frozen mobile contract). */
    public static boolean wantsJson(HttpServletRequest request) {
        if (request.getRequestURI().startsWith("/api/")) {
            return false;
        }
        if ("json".equalsIgnoreCase(request.getParameter("format"))) {
            return true;
        }
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("application/json") && !accept.contains("text/html");
    }
}
