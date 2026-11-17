package com.team01.freelance.contract.config;

import feign.RequestTemplate;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class FeignServiceIdentityTest {
    private static final String SECRET = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";
    private FeignCorrelationConfig config() {
        var config = new FeignCorrelationConfig();
        ReflectionTestUtils.setField(config, "jwtSecret", SECRET);
        return config;
    }
    @AfterEach void clearRequest() { RequestContextHolder.resetRequestAttributes(); }

    @Test void signsShortLivedServiceIdentityForConsumerRead() {
        var template = new RequestTemplate().method("GET");
        config().correlationIdInterceptor().apply(template);
        String header = template.headers().get("Authorization").iterator().next();
        var claims = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
                .build().parseSignedClaims(header.substring(7)).getPayload();
        assertEquals("contract-service", claims.getSubject());
        assertEquals("SERVICE", claims.get("role"));
        assertTrue(claims.getExpiration().toInstant().isBefore(Instant.now().plusSeconds(61)));
    }

    @Test void neverCreatesBackgroundWriteToken() {
        var template = new RequestTemplate().method("POST");
        config().correlationIdInterceptor().apply(template);
        assertFalse(template.headers().containsKey("Authorization"));
    }

    @Test void relaysHttpCallerInsteadOfServiceIdentity() {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer caller-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        var template = new RequestTemplate().method("GET");
        config().correlationIdInterceptor().apply(template);
        assertEquals("Bearer caller-token", template.headers().get("Authorization").iterator().next());
    }
}
