package com.team01.freelance.proposal.feign;

import com.team01.freelance.proposal.dto.FeignUserDTO;
import com.team01.freelance.proposal.feign.fallback.UserServiceClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "user-service",
        url = "${feign.user-service.url}",
        fallbackFactory = UserServiceClientFallbackFactory.class)
public interface UserServiceClient {

    @GetMapping("/api/users/{id}")
    FeignUserDTO getUser(@PathVariable("id") Long id);
}
