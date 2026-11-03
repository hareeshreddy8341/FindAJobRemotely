package com.findajobremotely.security;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientIp {

    private ClientIp() {
    }

    /**
     * Only honours X-Forwarded-For when explicitly enabled; otherwise any client could spoof its
     * address to dodge rate limiting. Enable it only behind a proxy that overwrites the header.
     */
    public static String resolve(HttpServletRequest req, boolean trustForwardedHeaders) {
        if (trustForwardedHeaders) {
            String xff = req.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                String first = xff.split(",")[0].trim();
                if (!first.isEmpty() && first.length() <= 45) {
                    return first;
                }
            }
        }
        return req.getRemoteAddr();
    }
}
