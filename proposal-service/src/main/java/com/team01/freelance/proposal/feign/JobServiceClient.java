package com.team01.freelance.proposal.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "job-service", url = "${feign.job-service.url}")
public interface JobServiceClient {

    // S3-F11, S3-F12: enrichment / recommended jobs
    @GetMapping("/api/jobs/{jobId}")
    Object getJobById(@PathVariable Long jobId);
}
