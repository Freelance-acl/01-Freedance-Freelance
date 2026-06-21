package com.team01.freelance.wallet.feign;

import com.team01.freelance.wallet.dto.ContractDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "contract-service", url = "${feign.contract-service.url}")
public interface ContractServiceClient {

    // S5-F4: validate contract status = COMPLETED before releasing payout
    @GetMapping("/api/contracts/{contractId}/status")
    String getContractStatus(@PathVariable Long contractId);

    // M3 Refactor: Replaces Object return type to specifically map required fields
    @GetMapping("/api/contracts/{contractId}")
    ContractDTO getContract(@PathVariable Long contractId);
}