package com.team01.freelance.user.support;

import com.team01.freelance.user.config.JwtConfigurationManager;
import com.team01.freelance.user.model.User;
import com.team01.freelance.user.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import java.util.Date;

public final class JwtTestSupport {

    private JwtTestSupport() {
    }

    public static String extractRoleClaim(String token) {
        return extractClaims(token).get(JwtService.ROLE_CLAIM, String.class);
    }

    public static Long extractUidClaim(String token) {
        Object uid = extractClaims(token).get(JwtService.UID_CLAIM);
        if (uid instanceof Number number) {
            return number.longValue();
        }
        return uid == null ? null : Long.valueOf(uid.toString());
    }

    public static String extractSubjectClaim(String token) {
        return extractClaims(token).getSubject();
    }

    /** Token with {@code exp} in the past for CC-1 expired-JWT scenarios. */
    public static String expiredToken(User user) {
        Date issuedAt = new Date(System.currentTimeMillis() - 120_000L);
        Date expiresAt = new Date(System.currentTimeMillis() - 60_000L);
        return Jwts.builder()
                .subject(user.getEmail())
                .claim(JwtService.UID_CLAIM, user.getId())
                .claim(JwtService.ROLE_CLAIM, user.getRole().name())
                .issuedAt(issuedAt)
                .expiration(expiresAt)
                .signWith(JwtConfigurationManager.getInstance().getSigningKey())
                .compact();
    }

    /** Backward-compatible overload for tests that still pass JwtConfig. */
    public static String expiredToken(User user, com.team01.freelance.user.config.JwtConfig jwtConfig) {
        JwtConfigurationManager.resetForTests(jwtConfig.getSecret(), jwtConfig.getExpiration());
        return expiredToken(user);
    }

    public static String extractRoleClaim(String token, com.team01.freelance.user.config.JwtConfig jwtConfig) {
        JwtConfigurationManager.resetForTests(jwtConfig.getSecret(), jwtConfig.getExpiration());
        return extractRoleClaim(token);
    }

    public static Long extractUidClaim(String token, com.team01.freelance.user.config.JwtConfig jwtConfig) {
        JwtConfigurationManager.resetForTests(jwtConfig.getSecret(), jwtConfig.getExpiration());
        return extractUidClaim(token);
    }

    public static String extractSubjectClaim(String token, com.team01.freelance.user.config.JwtConfig jwtConfig) {
        JwtConfigurationManager.resetForTests(jwtConfig.getSecret(), jwtConfig.getExpiration());
        return extractSubjectClaim(token);
    }

    private static Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(JwtConfigurationManager.getInstance().getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
