package com.team01.freelance.contracts.events;

import java.math.BigDecimal;

public record ProposalAcceptedEvent(
        Long proposalId,
        Long jobId,
        Long freelancerId,
        BigDecimal bidAmount
) {
    public static final String ROUTING_KEY = "proposal.accepted";
}
