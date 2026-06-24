package com.team01.freelance.contract.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

@Component
public class JwtTokenService {

    private final SecretKey signingKey;

    public JwtTokenService(@Value("${jwt.secret:freelance-platform-secret-key-at-least-32-bytes}") String secret) {
        byte[] raw = Decoders.BASE64.decode(secret);
        if (raw.length < 32) {
            throw new IllegalArgumentException("JWT secret must decode to at least 32 bytes");
        }
        this.signingKey = Keys.hmacShaKeyFor(raw);
    }

    public Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    }
}
