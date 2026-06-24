package com.team01.freelance.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.UUID;

@Component
public class JwtGatewayFilter implements GlobalFilter, Ordered {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }
        final String finalCorrelationId = correlationId;

        // Auth and health endpoints pass through without JWT.
        // Any path ending in /health is public (matches each service's PublicEndpoints rule):
        // /health (gateway shortcut) plus /api/users/health, /api/jobs/health, etc.
        if (path.startsWith("/api/auth/") || path.endsWith("/health")) {
            return chain.filter(exchange.mutate()
                    .request(r -> r.headers(h -> {
                        h.remove(USER_ID_HEADER);
                        h.remove(USER_ROLE_HEADER);
                        h.set(CORRELATION_ID_HEADER, finalCorrelationId);
                    }))
                    .build());
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            Claims claims = parseToken(authHeader.substring(7));
            String userId = claims.getSubject();
            String role = claims.get("role", String.class);
            final String finalRole = role != null ? role : "";

            // Strip any client-supplied X-User-* headers before setting gateway-derived values.
            // Without this, Spring Cloud Gateway's .header() appends rather than replaces, so a
            // client could inject X-User-Id/X-User-Role and have them read first by services.
            return chain.filter(exchange.mutate()
                    .request(r -> r.headers(h -> {
                        h.remove(USER_ID_HEADER);
                        h.remove(USER_ROLE_HEADER);
                        h.set(USER_ID_HEADER, userId);
                        h.set(USER_ROLE_HEADER, finalRole);
                        h.set(CORRELATION_ID_HEADER, finalCorrelationId);
                    }))
                    .build());
        } catch (Exception e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private Claims parseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
