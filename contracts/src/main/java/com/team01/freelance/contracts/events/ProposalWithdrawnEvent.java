package com.team01.freelance.contracts.events;

public record ProposalWithdrawnEvent(
        Long proposalId,
        Long jobId,
        Long freelancerId
) {
    public static final String ROUTING_KEY = "proposal.withdrawn";
}
