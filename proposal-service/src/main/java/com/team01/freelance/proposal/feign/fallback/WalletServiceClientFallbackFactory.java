package com.team01.freelance.proposal.feign.fallback;

import com.team01.freelance.proposal.feign.WalletServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class WalletServiceClientFallbackFactory implements FallbackFactory<WalletServiceClient> {

    private static final Logger log = LoggerFactory.getLogger(WalletServiceClientFallbackFactory.class);

    @Override
    public WalletServiceClient create(Throwable cause) {
        log.warn("WalletServiceClient circuit breaker open — returning false fallback. Cause: {}", cause.getMessage());
        return contractId -> false;
    }
}
