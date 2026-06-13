package com.team01.freelance.contracts.events;

import java.math.BigDecimal;

public record ContractCreatedEvent(
        Long contractId,
        Long proposalId,
        Long jobId,
        Long freelancerId,
        BigDecimal agreedAmount
) {
    public static final String ROUTING_KEY = "contract.created";
}
