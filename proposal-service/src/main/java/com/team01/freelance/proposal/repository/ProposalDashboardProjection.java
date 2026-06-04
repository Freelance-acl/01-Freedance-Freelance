package com.team01.freelance.proposal.repository;

public interface ProposalDashboardProjection {
    Long getTotalProposals();

    Long getAcceptedProposals();

    Double getAverageBidAmount();

    Double getAverageEstimatedDays();
}
