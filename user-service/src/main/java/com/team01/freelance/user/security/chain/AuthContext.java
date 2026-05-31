package com.team01.freelance.user.security.chain;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;

/**
 * Mutable state passed through the JWT {@link AuthHandler} chain.
 */
public class AuthContext {

    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private String token;
    private Authentication authentication;
    private Integer failureStatus;
    private String failureMessage;

    public AuthContext(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public HttpServletResponse getResponse() {
        return response;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }

    public boolean hasFailed() {
        return failureStatus != null;
    }

    public void fail(int status, String message) {
        this.failureStatus = status;
        this.failureMessage = message;
    }

    public int getFailureStatus() {
        return failureStatus == null ? 401 : failureStatus;
    }

    public String getFailureMessage() {
        return failureMessage;
    }
}
