package com.team01.freelance.proposal.config.security;

import org.springframework.http.HttpMethod;

/**
 * CC-1 public endpoints — health checks only in proposal-service.
 */
public final class PublicEndpoints {

    private PublicEndpoints() {
    }

    public static boolean isPublic(HttpMethod method, String path) {
        return path != null && path.endsWith("/health");
    }
}
