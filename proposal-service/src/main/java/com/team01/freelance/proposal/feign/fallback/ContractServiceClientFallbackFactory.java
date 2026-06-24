package com.team01.freelance.proposal.feign.fallback;

import com.team01.freelance.proposal.feign.ContractServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class ContractServiceClientFallbackFactory implements FallbackFactory<ContractServiceClient> {

    private static final Logger log = LoggerFactory.getLogger(ContractServiceClientFallbackFactory.class);

    @Override
    public ContractServiceClient create(Throwable cause) {
        log.warn("ContractServiceClient circuit breaker open — returning null fallback. Cause: {}", cause.getMessage());
        return proposalId -> null;
    }
}
