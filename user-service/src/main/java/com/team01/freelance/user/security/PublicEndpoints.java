package com.team01.freelance.user.security;

import org.springframework.http.HttpMethod;

/**
 * CC-1 public endpoints — register, login, and service health checks.
 * JWT validation is skipped for these paths so stale/invalid Bearer tokens cannot block them.
 */
public final class PublicEndpoints {

    public static final String REGISTER = "/api/auth/register";
    public static final String LOGIN = "/api/auth/login";

    private PublicEndpoints() {
    }

    public static boolean isPublic(HttpMethod method, String path) {
        if (path != null && path.endsWith("/health")) {
            return true;
        }
        return HttpMethod.POST.equals(method)
                && (REGISTER.equals(path) || LOGIN.equals(path));
    }
}
