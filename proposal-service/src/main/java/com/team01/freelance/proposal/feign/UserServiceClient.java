package com.team01.freelance.proposal.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "${feign.user-service.url}")
public interface UserServiceClient {

    // S3-F2: validate freelancer exists and is active before accepting proposal
    @GetMapping("/api/users/{userId}/exists")
    boolean userExists(@PathVariable Long userId);

    @GetMapping("/api/users/{userId}/active")
    boolean isUserActive(@PathVariable Long userId);

    // S3-F11, S3-F12: enrichment
    @GetMapping("/api/users/{userId}")
    Object getUserById(@PathVariable Long userId);
}
