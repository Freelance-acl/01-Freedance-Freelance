package com.team01.freelance.proposal.feign;

import com.team01.freelance.proposal.dto.FeignContractDTO;
import com.team01.freelance.proposal.feign.fallback.ContractServiceClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "contract-service",
        url = "${feign.contract-service.url}",
        fallbackFactory = ContractServiceClientFallbackFactory.class)
public interface ContractServiceClient {

    // S3-F4: pre-check — does an active contract exist for this proposal?
    @GetMapping("/api/contracts/proposal/{proposalId}/active")
    FeignContractDTO getActiveContract(@PathVariable Long proposalId);
}
