package com.team01.freelance.proposal.feign.fallback;

import com.team01.freelance.proposal.dto.FeignUserDTO;
import com.team01.freelance.proposal.feign.UserServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class UserServiceClientFallbackFactory implements FallbackFactory<UserServiceClient> {

    private static final Logger log = LoggerFactory.getLogger(UserServiceClientFallbackFactory.class);

    @Override
    public UserServiceClient create(Throwable cause) {
        log.warn("UserServiceClient circuit breaker open — returning fallback. Cause: {}", cause.getMessage());
        return userId -> {
            FeignUserDTO fallback = new FeignUserDTO();
            fallback.setId(userId);
            fallback.setName("Service Unavailable");
            fallback.setRole("UNKNOWN");
            fallback.setStatus("UNKNOWN");
            return fallback;
        };
    }
}
