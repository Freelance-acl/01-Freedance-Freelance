package com.team01.freelance.proposal.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "wallet-service", url = "${feign.wallet-service.url}")
public interface WalletServiceClient {

    // S3-F4: pre-check — is there already a pending payout for this contract?
    @GetMapping("/api/payouts/contract/{contractId}/exists")
    boolean payoutExistsForContract(@PathVariable Long contractId);
}
