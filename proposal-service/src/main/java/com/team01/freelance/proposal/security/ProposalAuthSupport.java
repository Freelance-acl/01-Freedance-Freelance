package com.team01.freelance.proposal.security;

import org.springframework.stereotype.Component;

import com.team01.freelance.user.service.JwtService;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class ProposalAuthSupport {

    private final JwtService jwtService;

    public ProposalAuthSupport(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public Long extractUid(HttpServletRequest request) {
        String token = extractBearerToken(request);
        if (token == null) {
            return null;
        }
        return jwtService.extractUserId(token);
    }

    public String extractRole(HttpServletRequest request) {
        String token = extractBearerToken(request);
        if (token == null) {
            return null;
        }
        return jwtService.extractRole(token);
    }

    private static String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        String token = header.substring(7).trim();
        return token.isEmpty() ? null : token;
    }
}
