package com.team01.freelance.wallet.strategy;

import com.team01.freelance.wallet.dto.MilestoneReversalRequest;
import com.team01.freelance.wallet.model.Payout;
import org.springframework.stereotype.Component;

@Component
public class NoReversalStrategy implements RefundStrategy {

    @Override
    public RefundResult calculateRefund(Payout payout, MilestoneReversalRequest request) {
        return new RefundResult(0.0, "REVERSAL_WINDOW_EXPIRED", "NoReversalStrategy");
    }
}

