package com.team01.freelance.proposal.feign;

import com.team01.freelance.proposal.dto.FeignJobDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "job-service", url = "${feign.job-service.url}")
public interface JobServiceClient {

    @GetMapping("/api/jobs/{jobId}")
    FeignJobDTO getJob(@PathVariable Long jobId);
}
