package com.team01.freelance.wallet.support.cc1;

import org.springframework.http.HttpMethod;

/**
 * CC-1 public endpoint rules: register, login, and health checks only.
 */
public final class Cc1PublicEndpoints {

    public static final String REGISTER = "/api/auth/register";
    public static final String LOGIN = "/api/auth/login";

    private Cc1PublicEndpoints() {
    }

    public static boolean isPublic(HttpMethod method, String path) {
        if (path != null && path.endsWith("/health")) {
            return true;
        }
        return HttpMethod.POST.equals(method)
                && (REGISTER.equals(path) || LOGIN.equals(path));
    }

    public static String category(HttpMethod method, String path) {
        if (path != null && path.endsWith("/health")) {
            return "health";
        }
        if (HttpMethod.POST.equals(method) && REGISTER.equals(path)) {
            return "register";
        }
        if (HttpMethod.POST.equals(method) && LOGIN.equals(path)) {
            return "login";
        }
        return null;
    }
}
