package com.team01.freelance.proposal.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "contract-service", url = "${feign.contract-service.url}")
public interface ContractServiceClient {

    // S3-F4: pre-check — does an active contract exist for this proposal?
    @GetMapping("/api/contracts/proposal/{proposalId}/active")
    boolean hasActiveContract(@PathVariable Long proposalId);
}
