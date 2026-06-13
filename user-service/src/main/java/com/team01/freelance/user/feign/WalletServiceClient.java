package com.team01.freelance.user.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;

@FeignClient(name = "wallet-service", url = "${feign.wallet-service.url}")
public interface WalletServiceClient {

    // S1-F6: total earnings for a freelancer in a date range
    @GetMapping("/api/payouts/freelancer/{freelancerId}/total")
    BigDecimal getFreelancerTotalEarnings(
            @PathVariable Long freelancerId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    );
}
