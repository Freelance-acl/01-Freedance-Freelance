package com.team01.freelance.wallet.strategy;

import com.team01.freelance.wallet.dto.MilestoneReversalRequest;
import com.team01.freelance.wallet.model.Payout;
import com.team01.freelance.wallet.repository.PayoutRepository;
import org.springframework.stereotype.Component;

@Component
public class MilestoneReversalStrategy implements RefundStrategy {

    private final PayoutRepository payoutRepository;

    public MilestoneReversalStrategy(PayoutRepository payoutRepository) {
        this.payoutRepository = payoutRepository;
    }

    @Override
    public RefundResult calculateRefund(Payout payout, MilestoneReversalRequest request) {
        Double sum = payoutRepository.sumIncompleteMilestoneAmounts(payout.getContractId());
        double amount = sum == null ? 0.0 : sum;
        return new RefundResult(amount, "MILESTONE_REVERSAL", "MilestoneReversalStrategy");
    }
}

