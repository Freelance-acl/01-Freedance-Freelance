package com.team01.freelance.proposal.support;

import com.team01.freelance.proposal.config.security.JwtConfigurationManager;
import com.team01.freelance.proposal.config.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import java.util.Date;

public final class ProposalJwtTestSupport {

    private ProposalJwtTestSupport() {
    }

    public static String bearer(String token) {
        return "Bearer " + token;
    }

    public static String token(JwtService jwtService, String email, Long uid, String role) {
        return jwtService.generateToken(email, uid, role);
    }

    public static String expiredToken(String email, Long uid, String role) {
        Date issuedAt = new Date(System.currentTimeMillis() - 120_000L);
        Date expiresAt = new Date(System.currentTimeMillis() - 60_000L);
        return Jwts.builder()
                .subject(email)
                .claim(JwtService.UID_CLAIM, uid)
                .claim(JwtService.ROLE_CLAIM, role)
                .issuedAt(issuedAt)
                .expiration(expiresAt)
                .signWith(JwtConfigurationManager.getInstance().getSigningKey())
                .compact();
    }

    public static String extractRoleClaim(String token) {
        return extractClaims(token).get(JwtService.ROLE_CLAIM, String.class);
    }

    private static Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(JwtConfigurationManager.getInstance().getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
