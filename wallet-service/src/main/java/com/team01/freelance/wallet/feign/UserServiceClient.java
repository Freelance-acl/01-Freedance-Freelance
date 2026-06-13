package com.team01.freelance.wallet.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "${feign.user-service.url}")
public interface UserServiceClient {

    // S5-F3: verify freelancer exists
    @GetMapping("/api/users/{userId}/exists")
    boolean userExists(@PathVariable Long userId);
}
