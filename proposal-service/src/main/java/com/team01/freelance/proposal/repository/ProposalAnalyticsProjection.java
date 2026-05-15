package com.team01.freelance.proposal.repository;

public interface ProposalAnalyticsProjection {
    Number getTotalProposals();

    Number getAcceptedProposals();

    Number getRejectedProposals();

    Number getTotalBidValue();

    Number getAverageBid();

    Number getAcceptanceRate();
}
