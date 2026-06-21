package com.team01.freelance.wallet.feign;

import com.team01.freelance.wallet.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "${feign.user-service.url}")
public interface UserServiceClient {

    // M3: fetch full user — throws FeignException.NotFound when user does not exist
    @GetMapping("/api/users/{userId}")
    UserDTO getUser(@PathVariable Long userId);

    // S5-F3 fallback: lightweight existence check
    @GetMapping("/api/users/{userId}/exists")
    boolean userExists(@PathVariable Long userId);
}
