package com.team01.freelance.proposal.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * Issues and validates HMAC-SHA256 (HS256) JWTs for proposal-service.
 */
@Service
public class JwtService {

    public static final String ROLE_CLAIM = "role";
    public static final String UID_CLAIM = "uid";

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    public String generateToken(String email, Long uid, String role) {
        JwtConfigurationManager config = JwtConfigurationManager.getInstance();
        Date now = new Date();
        return Jwts.builder()
                .subject(email)
                .claim(UID_CLAIM, uid)
                .claim(ROLE_CLAIM, role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + config.getExpiration()))
                .signWith(config.getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        Object uid = extractClaims(token).get(UID_CLAIM);
        if (uid == null) {
            return null;
        }
        if (uid instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(uid.toString());
    }

    public String extractRole(String token) {
        return extractClaims(token).get(ROLE_CLAIM, String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (JwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(JwtConfigurationManager.getInstance().getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
