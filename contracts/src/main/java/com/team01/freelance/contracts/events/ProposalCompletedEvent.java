package com.team01.freelance.contracts.events;

import java.math.BigDecimal;

public record ProposalCompletedEvent(
        Long proposalId,
        Long jobId,
        Long freelancerId,
        Long contractId,
        BigDecimal agreedAmount
) {
    public static final String ROUTING_KEY = "proposal.completed";
}
