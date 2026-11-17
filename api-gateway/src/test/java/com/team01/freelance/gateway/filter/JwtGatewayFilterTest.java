package com.team01.freelance.gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class JwtGatewayFilterTest {
    @Test void forwardsNumericUidAndOverwritesUntrustedIdentityHeaders() {
        String secret = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";
        String token = Jwts.builder().subject("client@example.com").claim("uid", 42L)
                .claim("role", "CLIENT").signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))).compact();
        var filter = new JwtGatewayFilter();
        ReflectionTestUtils.setField(filter, "jwtSecret", secret);
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/payouts")
                .header("Authorization", "Bearer " + token).header("X-User-Id", "spoofed")
                .header("X-User-Role", "ADMIN"));
        var forwarded = new AtomicReference<org.springframework.web.server.ServerWebExchange>();
        filter.filter(exchange, next -> { forwarded.set(next); return Mono.empty(); }).block();
        assertNotNull(forwarded.get());
        assertEquals("42", forwarded.get().getRequest().getHeaders().getFirst("X-User-Id"));
        assertEquals("CLIENT", forwarded.get().getRequest().getHeaders().getFirst("X-User-Role"));
    }
}
