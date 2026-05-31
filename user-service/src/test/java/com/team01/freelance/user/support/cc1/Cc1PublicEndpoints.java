package com.team01.freelance.user.support.cc1;

import com.team01.freelance.user.security.PublicEndpoints;
import org.springframework.http.HttpMethod;

/**
 * CC-1 public endpoint rules: register, login, and health checks only.
 */
public final class Cc1PublicEndpoints {

    public static final String REGISTER = PublicEndpoints.REGISTER;
    public static final String LOGIN = PublicEndpoints.LOGIN;

    private Cc1PublicEndpoints() {
    }

    public static boolean isPublic(HttpMethod method, String path) {
        return PublicEndpoints.isPublic(method, path);
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
