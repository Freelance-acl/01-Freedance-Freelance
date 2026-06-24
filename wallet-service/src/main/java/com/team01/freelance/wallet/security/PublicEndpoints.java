package com.team01.freelance.wallet.security;

import org.springframework.http.HttpMethod;

public final class PublicEndpoints {

    private PublicEndpoints() {
    }

    public static boolean isPublic(HttpMethod method, String path) {
        return path != null && path.endsWith("/health");
    }
}
