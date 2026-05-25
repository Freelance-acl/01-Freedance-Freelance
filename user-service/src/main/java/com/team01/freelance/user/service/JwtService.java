package com.team01.freelance.user.service;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.team01.freelance.user.config.JwtConfig;
import com.team01.freelance.user.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;

/**
 * Issues and validates HMAC-SHA256 (HS256) JWTs. Payload per §5.2: {@code sub} (email),
 * {@code uid} (numeric user id), {@code role}, {@code iat}, {@code exp}.
 */
@Service
public class JwtService {

    public static final String ROLE_CLAIM = "role";
    public static final String UID_CLAIM = "uid";

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final JwtConfig jwtConfig;
    private final SecretKey signingKey;

    public JwtService(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
        this.signingKey = buildSigningKey(jwtConfig.getSecret());
    }

    public String generateToken(User user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim(UID_CLAIM, user.getId())
                .claim(ROLE_CLAIM, user.getRole().name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + jwtConfig.getExpiration()))
                .signWith(signingKey)
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
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private static SecretKey buildSigningKey(String base64Secret) {
        if (base64Secret == null || base64Secret.isBlank()) {
            throw new IllegalStateException(
                    "jwt.secret is not set; provide JWT_SECRET (Base64, at least 32 bytes when decoded)");
        }
        byte[] keyBytes = Decoders.BASE64.decode(base64Secret);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "jwt.secret must decode to at least 32 bytes (256 bits) for HS256; "
                            + "use a 44-character Base64-encoded random key");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
