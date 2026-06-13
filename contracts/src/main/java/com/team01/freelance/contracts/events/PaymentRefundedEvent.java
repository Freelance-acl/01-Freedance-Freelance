package com.team01.freelance.contracts.events;

import java.math.BigDecimal;

public record PaymentRefundedEvent(
        Long payoutId,
        Long proposalId,
        Long contractId,
        BigDecimal refundAmount
) {
    public static final String ROUTING_KEY = "payment.refunded";
}
