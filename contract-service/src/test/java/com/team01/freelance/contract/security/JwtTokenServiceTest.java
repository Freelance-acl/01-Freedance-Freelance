package com.team01.freelance.contract.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JwtTokenServiceTest {
    @Test void acceptsTheSameBase64SigningKeyAsUserServiceAndGateway() {
        String secret = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";
        String token = Jwts.builder().subject("demo@example.com").claim("uid", 1L)
                .claim("role", "CLIENT").signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))).compact();
        assertEquals("demo@example.com", new JwtTokenService(secret).parseClaims(token).getSubject());
    }

    @Test void rejectsAnUndersizedDecodedKey() {
        assertThrows(IllegalArgumentException.class, () -> new JwtTokenService("c2hvcnQ="));
    }
}
