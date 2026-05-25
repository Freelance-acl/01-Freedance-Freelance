package com.team01.freelance.user.support;

import com.team01.freelance.user.config.JwtConfig;
import com.team01.freelance.user.model.User;
import com.team01.freelance.user.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;

public final class JwtTestSupport {

    private JwtTestSupport() {
    }

    public static String extractRoleClaim(String token, JwtConfig jwtConfig) {
        return extractClaims(token, jwtConfig).get(JwtService.ROLE_CLAIM, String.class);
    }

    public static Long extractUidClaim(String token, JwtConfig jwtConfig) {
        Object uid = extractClaims(token, jwtConfig).get(JwtService.UID_CLAIM);
        if (uid instanceof Number number) {
            return number.longValue();
        }
        return uid == null ? null : Long.valueOf(uid.toString());
    }

    public static String extractSubjectClaim(String token, JwtConfig jwtConfig) {
        return extractClaims(token, jwtConfig).getSubject();
    }

    /** Token with {@code exp} in the past for CC-1 expired-JWT scenarios. */
    public static String expiredToken(User user, JwtConfig jwtConfig) {
        Date issuedAt = new Date(System.currentTimeMillis() - 120_000L);
        Date expiresAt = new Date(System.currentTimeMillis() - 60_000L);
        return Jwts.builder()
                .subject(user.getEmail())
                .claim(JwtService.UID_CLAIM, user.getId())
                .claim(JwtService.ROLE_CLAIM, user.getRole().name())
                .issuedAt(issuedAt)
                .expiration(expiresAt)
                .signWith(signingKey(jwtConfig))
                .compact();
    }

    private static Claims extractClaims(String token, JwtConfig jwtConfig) {
        return Jwts.parser()
                .verifyWith(signingKey(jwtConfig))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private static SecretKey signingKey(JwtConfig jwtConfig) {
        byte[] keyBytes = Decoders.BASE64.decode(jwtConfig.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
