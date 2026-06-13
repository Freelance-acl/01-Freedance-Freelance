package com.team01.freelance.contracts.events;

public record PaymentFailedEvent(
        Long payoutId,
        Long proposalId,
        Long contractId,
        String reason
) {
    public static final String ROUTING_KEY = "payment.failed";
}
