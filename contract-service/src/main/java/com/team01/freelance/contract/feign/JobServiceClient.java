package com.team01.freelance.contract.feign;

import com.team01.freelance.job.model.Job;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "job-service", url = "${feign.job-service.url}")
public interface JobServiceClient {

    @GetMapping("/api/jobs/{id}")
    Job getJob(@PathVariable Long id);
}
