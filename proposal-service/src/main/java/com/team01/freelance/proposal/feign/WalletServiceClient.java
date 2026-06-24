package com.team01.freelance.proposal.feign;

import com.team01.freelance.proposal.feign.fallback.WalletServiceClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "wallet-service",
        url = "${feign.wallet-service.url}",
        fallbackFactory = WalletServiceClientFallbackFactory.class)
public interface WalletServiceClient {

    // S3-F4: pre-check — is there already a pending payout for this contract?
    @GetMapping("/api/payouts/contract/{contractId}/exists")
    boolean payoutExistsForContract(@PathVariable Long contractId);
}
