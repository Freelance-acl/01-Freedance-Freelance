package com.team01.freelance.wallet.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "contract-service", url = "${feign.contract-service.url}")
public interface ContractServiceClient {

    // S5-F4: validate contract status = COMPLETED before releasing payout
    @GetMapping("/api/contracts/{contractId}/status")
    String getContractStatus(@PathVariable Long contractId);

    // S5-F10: get contract details for category-based analytics
    @GetMapping("/api/contracts/{contractId}")
    Object getContractById(@PathVariable Long contractId);
}
