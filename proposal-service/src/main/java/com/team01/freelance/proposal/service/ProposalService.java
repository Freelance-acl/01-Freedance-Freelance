package com.team01.freelance.proposal.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.team01.freelance.contract.model.Contract;
import com.team01.freelance.contract.repository.ContractRepository;
import com.team01.freelance.job.model.Job;
import com.team01.freelance.job.repository.JobRepository;
import com.team01.freelance.wallet.repository.PayoutRepository;
import com.team01.freelance.proposal.model.Proposal;
import com.team01.freelance.proposal.model.ProposalStatus;
import com.team01.freelance.proposal.repository.ProposalRepository;
import com.team01.freelance.user.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class ProposalService {

    private static final Set<ProposalStatus> ACCEPTABLE_STATUSES = EnumSet.of(
            ProposalStatus.SUBMITTED,
            ProposalStatus.SHORTLISTED
    );

    @Autowired
    private ProposalRepository proposalRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private PayoutRepository payoutRepository;

    public List<Proposal> getAllProposals() {
        return proposalRepository.findAll();
    }

    public Optional<Proposal> getProposalById(Long id) {
        return proposalRepository.findById(id);
    }

    /**
     * Finds proposals whose {@code submittedAt} falls on or after {@code startDate} and on or before {@code endDate}
     * (inclusive calendar days), optionally filtered by status. Results are ordered by {@code submittedAt} descending.
     *
     * @param status optional status filter; null or blank means any status
     */
    public List<Proposal> searchProposals(String status, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("startDate and endDate are required");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate must be on or before endDate");
        }
        ProposalStatus parsedStatus = null;
        if (status != null && !status.isBlank()) {
            parsedStatus = ProposalStatus.fromString(status.trim());
        }
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime endExclusive = endDate.plusDays(1).atStartOfDay();
        return proposalRepository.searchBySubmittedAtRangeAndOptionalStatus(start, endExclusive, parsedStatus);
    }

    public Proposal createProposal(Proposal proposal) {
        if (proposal.getFreelancerId() == null || proposal.getJobId() == null) {
            throw new IllegalArgumentException("Freelancer and Job IDs are required to create a Proposal");
        }

        jobRepository.findById(proposal.getJobId())
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + proposal.getJobId()));

        userRepository.findById(proposal.getFreelancerId())
                .orElseThrow(() -> new EntityNotFoundException("Freelancer not found with id: " + proposal.getFreelancerId()));

        return proposalRepository.save(proposal);
    }

    /**
     * Updates an existing proposal and throws if it does not exist.
     *
     * @param id The ID of the proposal to update
     * @param proposalDetails The object containing updated fields
     * @return The updated proposal
     * @throws EntityNotFoundException if the proposal is not found
     */
    public Proposal updateProposal(Long id, Proposal proposalDetails) {
        return proposalRepository.findById(id).map(existingProposal -> {
            if (proposalDetails.getCoverLetter() != null) existingProposal.setCoverLetter(proposalDetails.getCoverLetter());
            if (proposalDetails.getBidAmount() != null) existingProposal.setBidAmount(proposalDetails.getBidAmount());
            if (proposalDetails.getEstimatedDays() != null) existingProposal.setEstimatedDays(proposalDetails.getEstimatedDays());
            if (proposalDetails.getStatus() != null) existingProposal.setStatus(proposalDetails.getStatus());
            if (proposalDetails.getMetadata() != null) existingProposal.setMetadata(proposalDetails.getMetadata());
            if (proposalDetails.getAcceptedAt() != null) existingProposal.setAcceptedAt(proposalDetails.getAcceptedAt());
            return proposalRepository.save(existingProposal);
        }).orElseThrow(() -> new EntityNotFoundException("Proposal not found with id: " + id));
    }

    /**
     * Accepts a proposal, marks the job in progress, and creates an active contract transactionally.
     *
     * @param id the proposal ID
     * @return the accepted proposal
     * @throws EntityNotFoundException if the proposal, job, or freelancer user is not found
     * @throws IllegalArgumentException if the proposal status is not acceptable or the user is not a freelancer
     */
    @Transactional
    public Proposal acceptProposal(Long id) {
        Proposal proposal = proposalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Proposal not found with id: " + id));

        if (!ACCEPTABLE_STATUSES.contains(proposal.getStatus())) {
            throw new IllegalArgumentException("Only SUBMITTED or SHORTLISTED proposals can be accepted");
        }

        String freelancerRole = userRepository.findRoleByUserId(proposal.getFreelancerId());
        if (freelancerRole == null) {
            throw new EntityNotFoundException("Freelancer not found with id: " + proposal.getFreelancerId());
        }
        if (!"FREELANCER".equalsIgnoreCase(freelancerRole)) {
            throw new IllegalArgumentException("User is not a freelancer");
        }

        Job job = jobRepository.findById(proposal.getJobId())
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + proposal.getJobId()));

        LocalDateTime now = LocalDateTime.now();
        proposal.setStatus(ProposalStatus.ACCEPTED);
        proposal.setAcceptedAt(now);
        Proposal acceptedProposal = proposalRepository.save(proposal);

        jobRepository.markJobInProgress(proposal.getJobId());

        contractRepository.insertActiveContract(
                proposal.getJobId(),
                proposal.getFreelancerId(),
                job.getClientId(),
                proposal.getId(),
                proposal.getBidAmount(),
                now
        );

        return acceptedProposal;
    }

    /**
     * Completes work for an accepted proposal: closes the active contract, closes the job,
     * and creates a pending payout transactionally.
     *
     * @param id the proposal ID
     * @return the proposal (status remains ACCEPTED)
     * @throws EntityNotFoundException if the proposal is not found
     * @throws IllegalArgumentException if the proposal is not ACCEPTED or has no ACTIVE contract
     */
    @Transactional
    public Proposal completeProposal(Long id) {
        Proposal proposal = proposalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Proposal not found with id: " + id));

        if (proposal.getStatus() != ProposalStatus.ACCEPTED) {
            throw new IllegalArgumentException("Only ACCEPTED proposals can be completed");
        }

        Contract contract = contractRepository.findActiveContractByProposalId(id)
                .orElseThrow(() -> new IllegalArgumentException("No ACTIVE contract found for proposal"));

        LocalDateTime now = LocalDateTime.now();
        contractRepository.completeActiveContract(contract.getId(), now);
        jobRepository.markJobClosed(contract.getJobId());
        payoutRepository.insertPendingPayout(
                contract.getId(),
                contract.getFreelancerId(),
                contract.getAgreedAmount(),
                now
        );

        return proposal;
    }

    public boolean deleteProposalById(Long id) {
        if (!proposalRepository.existsById(id)) {
            return false;
        }
        proposalRepository.deleteById(id);
        return true;
    }

    public void deleteAllProposals() {
        proposalRepository.deleteAll();
    }
}
