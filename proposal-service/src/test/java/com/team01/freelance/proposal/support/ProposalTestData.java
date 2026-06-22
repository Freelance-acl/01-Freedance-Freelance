package com.team01.freelance.proposal.support;

import com.team01.freelance.proposal.model.Proposal;
import com.team01.freelance.proposal.model.ProposalStatus;
import com.team01.freelance.proposal.repository.ProposalRepository;
import java.time.LocalDateTime;

public final class ProposalTestData {

    public static final long JOB_ID = 100L;
    public static final long FREELANCER_ID = 200L;
    public static final long CLIENT_ID = 1L;

    private ProposalTestData() {
    }

    public static Proposal saveProposal(ProposalRepository repository, ProposalStatus status, double bidAmount) {
        Proposal proposal = new Proposal();
        proposal.setJobId(JOB_ID);
        proposal.setFreelancerId(FREELANCER_ID);
        proposal.setCoverLetter("Integration test proposal");
        proposal.setBidAmount(bidAmount);
        proposal.setEstimatedDays(14);
        proposal.setStatus(status);
        proposal.setSubmittedAt(LocalDateTime.now());
        if (status == ProposalStatus.ACCEPTED) {
            proposal.setAcceptedAt(LocalDateTime.now());
        }
        return repository.save(proposal);
    }

    public static Proposal saveProposal(ProposalRepository repository, Long jobId, Long freelancerId,
                                        ProposalStatus status, double bidAmount) {
        Proposal proposal = new Proposal();
        proposal.setJobId(jobId);
        proposal.setFreelancerId(freelancerId);
        proposal.setCoverLetter("Integration test proposal");
        proposal.setBidAmount(bidAmount);
        proposal.setEstimatedDays(14);
        proposal.setStatus(status);
        proposal.setSubmittedAt(LocalDateTime.now());
        if (status == ProposalStatus.ACCEPTED) {
            proposal.setAcceptedAt(LocalDateTime.now());
        }
        return repository.save(proposal);
    }
}
