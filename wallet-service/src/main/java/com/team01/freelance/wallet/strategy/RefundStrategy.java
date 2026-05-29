package com.team01.freelance.wallet.strategy;

import com.team01.freelance.wallet.dto.MilestoneReversalRequest;
import com.team01.freelance.wallet.model.Payout;

/** DP-1 Strategy — calculates refund amount for milestone-based reversal. */
public interface RefundStrategy {

    RefundResult calculateRefund(Payout payout, MilestoneReversalRequest request);
}

