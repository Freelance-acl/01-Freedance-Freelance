package com.team01.freelance.wallet.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "job-service", url = "${feign.job-service.url}")
public interface JobServiceClient {

    // S5-F10: get job category for platform fee analytics
    @GetMapping("/api/jobs/{jobId}")
    Object getJobById(@PathVariable Long jobId);
}
