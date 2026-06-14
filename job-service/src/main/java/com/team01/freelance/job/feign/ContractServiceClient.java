package com.team01.freelance.job.feign;

import com.team01.freelance.job.feign.dto.ContractResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "contract-service", url = "${feign.contract-service.url}")
public interface ContractServiceClient {

    @GetMapping("/api/contracts/job/{jobId}/active-count")
    Integer getActiveContractCountForJob(@PathVariable("jobId") Long jobId);

    @GetMapping("/api/contracts/{contractId}")
    ContractResponse getContractById(@PathVariable("contractId") Long contractId);
}
