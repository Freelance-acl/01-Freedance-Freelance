package com.team01.freelance.contracts.events;

import java.math.BigDecimal;

public record PaymentCompletedEvent(
        Long payoutId,
        Long proposalId,
        Long contractId,
        BigDecimal amount
) {
    public static final String ROUTING_KEY = "payment.completed";
}
