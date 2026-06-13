package com.team01.freelance.contract.feign;

import com.team01.freelance.user.model.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "${feign.user-service.url}")
public interface UserServiceClient {

    @GetMapping("/api/users/{id}")
    User getUser(@PathVariable Long id);
}
