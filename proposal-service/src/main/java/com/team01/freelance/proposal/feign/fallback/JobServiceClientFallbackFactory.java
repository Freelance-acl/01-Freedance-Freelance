package com.team01.freelance.proposal.feign.fallback;

import com.team01.freelance.proposal.dto.FeignJobDTO;
import com.team01.freelance.proposal.feign.JobServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class JobServiceClientFallbackFactory implements FallbackFactory<JobServiceClient> {

    private static final Logger log = LoggerFactory.getLogger(JobServiceClientFallbackFactory.class);

    @Override
    public JobServiceClient create(Throwable cause) {
        log.warn("JobServiceClient circuit breaker open — returning fallback. Cause: {}", cause.getMessage());
        return jobId -> {
            FeignJobDTO fallback = new FeignJobDTO();
            fallback.setId(jobId);
            fallback.setStatus("UNKNOWN");
            fallback.setTitle("Unavailable");
            return fallback;
        };
    }
}
