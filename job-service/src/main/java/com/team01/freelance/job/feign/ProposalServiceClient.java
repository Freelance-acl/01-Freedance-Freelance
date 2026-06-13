package com.team01.freelance.job.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "proposal-service", url = "${feign.proposal-service.url}")
public interface ProposalServiceClient {

    // S2-F3: job proposal summary
    @GetMapping("/api/proposals/job/{jobId}/summary")
    Object getJobProposalSummary(@PathVariable Long jobId);

    // S2-F12: proposal counts per job for market dashboard
    @GetMapping("/api/proposals/job/{jobId}/count")
    long getProposalCountByJob(@PathVariable Long jobId);
}
