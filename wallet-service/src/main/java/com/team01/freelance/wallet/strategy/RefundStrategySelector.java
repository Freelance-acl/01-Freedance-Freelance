package com.team01.freelance.wallet.strategy;

import com.team01.freelance.wallet.dto.MilestoneReversalRequest;
import com.team01.freelance.wallet.model.Payout;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Component
public class RefundStrategySelector {

    private static final long REVERSAL_WINDOW_DAYS = 30;

    private final FullPayoutReversalStrategy fullPayoutReversalStrategy;
    private final MilestoneReversalStrategy milestoneReversalStrategy;
    private final NoReversalStrategy noReversalStrategy;

    public RefundStrategySelector(
            FullPayoutReversalStrategy fullPayoutReversalStrategy,
            MilestoneReversalStrategy milestoneReversalStrategy,
            NoReversalStrategy noReversalStrategy) {
        this.fullPayoutReversalStrategy = fullPayoutReversalStrategy;
        this.milestoneReversalStrategy = milestoneReversalStrategy;
        this.noReversalStrategy = noReversalStrategy;
    }

    public RefundStrategy select(Payout payout, MilestoneReversalRequest request) {
        if (isReversalWindowExpired(payout)) {
            return noReversalStrategy;
        }
        String scope = request != null ? request.getReversalScope() : null;
        if ("FULL".equalsIgnoreCase(scope)) {
            return fullPayoutReversalStrategy;
        }
        if ("MILESTONE_ONLY".equalsIgnoreCase(scope)) {
            return milestoneReversalStrategy;
        }
        throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST,
                "reversalScope must be FULL or MILESTONE_ONLY");
    }

    private static boolean isReversalWindowExpired(Payout payout) {
        LocalDateTime createdAt = payout.getCreatedAt();
        if (createdAt == null) {
            return true;
        }
        return ChronoUnit.DAYS.between(createdAt, LocalDateTime.now()) > REVERSAL_WINDOW_DAYS;
    }
}
