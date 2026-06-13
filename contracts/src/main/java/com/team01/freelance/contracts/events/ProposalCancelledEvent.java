package com.team01.freelance.contracts.events;

public record ProposalCancelledEvent(
        Long proposalId,
        Long jobId,
        Long freelancerId,
        String reason
) {
    public static final String ROUTING_KEY = "proposal.cancelled";
}
