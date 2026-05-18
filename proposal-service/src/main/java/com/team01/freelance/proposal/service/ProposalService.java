package com.team01.freelance.proposal.service;

import com.team01.freelance.proposal.dto.FeeEstimateDTO;
import com.team01.freelance.proposal.model.Proposal;
import com.team01.freelance.proposal.model.ProposalStatus;
import com.team01.freelance.proposal.repository.ProposalRepository;
import com.team01.freelance.job.repository.JobRepository;
import com.team01.freelance.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ProposalService {

    @Autowired
    private ProposalRepository proposalRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

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

    /**
     * Computes a read-only platform fee estimate from bid amount and duration.
     *
     * @param bidAmount the proposed bid (must be positive)
     * @param estimatedDays expected delivery days (must be positive)
     * @return fee breakdown for the freelancer
     * @throws IllegalArgumentException if inputs are null or not positive
     */
    public FeeEstimateDTO estimatePlatformFee(Double bidAmount, Integer estimatedDays) {
        if (bidAmount == null || bidAmount <= 0) {
            throw new IllegalArgumentException("bidAmount must be positive");
        }
        if (estimatedDays == null || estimatedDays <= 0) {
            throw new IllegalArgumentException("estimatedDays must be positive");
        }

        double minBid = bidAmount * 0.8;
        double maxBid = bidAmount * 1.2;
        long similarProposalCount = proposalRepository.countActiveProposalsInSimilarBidRange(minBid, maxBid);
        int feePercentage = resolveFeePercentage(similarProposalCount);

        double platformFee = bidAmount * feePercentage / 100.0;
        double freelancerPayout = bidAmount - platformFee;
        double estimatedDailyRate = freelancerPayout / estimatedDays;

        return new FeeEstimateDTO(
                bidAmount,
                platformFee,
                freelancerPayout,
                feePercentage,
                estimatedDailyRate);
    }

    private static int resolveFeePercentage(long similarProposalCount) {
        if (similarProposalCount <= 5) {
            return 20;
        }
        if (similarProposalCount <= 15) {
            return 15;
        }
        return 10;
    }
}
