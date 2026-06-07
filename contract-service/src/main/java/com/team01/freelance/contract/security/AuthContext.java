package com.team01.freelance.contract.security;

import com.team01.freelance.user.model.User;
import com.team01.freelance.user.model.UserRole;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;

public class AuthContext {

    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private String token;
    private Claims claims;
    private Long userId;
    private UserRole role;
    private User user;
    private Set<UserRole> requiredRoles = EnumSet.allOf(UserRole.class);

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

    public Claims getClaims() {
        return claims;
    }

    public void setClaims(Claims claims) {
        this.claims = claims;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Set<UserRole> getRequiredRoles() {
        return requiredRoles;
    }

    public void setRequiredRoles(Set<UserRole> requiredRoles) {
        this.requiredRoles = requiredRoles;
    }

    public void unauthorized(String message) throws IOException {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, message);
    }

    public void forbidden(String message) throws IOException {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, message);
    }
}
