package com.team01.freelance.job.feign;

import com.team01.freelance.job.feign.dto.ProposalJobSummaryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@FeignClient(name = "proposal-service", url = "${feign.proposal-service.url}")
public interface ProposalServiceClient {

    @GetMapping("/api/proposals/job/{jobId}/summary")
    ProposalJobSummaryResponse getJobProposalSummary(
            @PathVariable("jobId") Long jobId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate);

    @GetMapping("/api/proposals/job/{jobId}/active-count")
    Integer getActiveProposalCountForJob(@PathVariable("jobId") Long jobId);
}
