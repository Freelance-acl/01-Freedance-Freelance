package com.team01.freelance.contract.config;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import feign.RequestInterceptor;

@Configuration
public class FeignCorrelationConfig {
    @org.springframework.beans.factory.annotation.Value("${jwt.secret}")
    private String jwtSecret;


    @Bean
    public RequestInterceptor correlationIdInterceptor() {
        return template -> {
            // Preserve the signed caller identity for synchronous service calls.
            if (!template.headers().containsKey("Authorization")
                    && RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
                String authorization = attributes.getRequest().getHeader("Authorization");
                if (authorization != null) template.header("Authorization", authorization);
            }
            // RabbitMQ consumers have no HTTP caller. Issue a short-lived service
            // identity for their read-only lookup; never impersonate an end user.
            if (!template.headers().containsKey("Authorization") && "GET".equals(template.method())) {
                java.time.Instant now = java.time.Instant.now();
                String token = io.jsonwebtoken.Jwts.builder()
                        .subject("contract-service")
                        .claim("uid", 0L).claim("role", "SERVICE")
                        .issuedAt(java.util.Date.from(now))
                        .expiration(java.util.Date.from(now.plusSeconds(60)))
                        .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(io.jsonwebtoken.io.Decoders.BASE64.decode(jwtSecret)))
                        .compact();
                template.header("Authorization", "Bearer " + token);
            }
            String correlationId = MDC.get("correlationId");
            if (correlationId != null) {
                template.header("X-Correlation-ID", correlationId);
            }
        };
    }
}