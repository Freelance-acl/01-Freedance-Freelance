package com.team01.freelance.job.config;

import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;

class FeignAuthRelayTest {
    @AfterEach void clearRequest() { RequestContextHolder.resetRequestAttributes(); }

    @Test void forwardsCallerTokenToDownstreamService() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer caller-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        RequestTemplate template = new RequestTemplate();
        new FeignCorrelationConfig().correlationIdInterceptor().apply(template);
        assertEquals("Bearer caller-token", template.headers().get("Authorization").iterator().next());
    }

    @Test void preservesExplicitAuthorizationAndDoesNotInventUserIdentity() {
        RequestTemplate template = new RequestTemplate();
        new FeignCorrelationConfig().correlationIdInterceptor().apply(template);
        assertFalse(template.headers().containsKey("Authorization"));
        template.header("Authorization", "Bearer explicit-token");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer caller-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        new FeignCorrelationConfig().correlationIdInterceptor().apply(template);
        assertEquals(1, template.headers().get("Authorization").size());
        assertEquals("Bearer explicit-token", template.headers().get("Authorization").iterator().next());
    }
}
