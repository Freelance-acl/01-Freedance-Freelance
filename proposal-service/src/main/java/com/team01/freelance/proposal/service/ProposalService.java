package com.team01.freelance.proposal.service;

import com.team01.freelance.proposal.model.Proposal;
import com.team01.freelance.proposal.model.ProposalMilestone;
import com.team01.freelance.proposal.model.ProposalStatus;
import com.team01.freelance.proposal.model.MilestoneStatus;
import com.team01.freelance.proposal.repository.ProposalRepository;
import com.team01.freelance.job.repository.JobRepository;
import com.team01.freelance.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class ProposalService {

    private static final List<ProposalStatus> MILESTONE_ALLOWED_STATUSES = List.of(
            ProposalStatus.SUBMITTED,
            ProposalStatus.SHORTLISTED);

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

    @Transactional
    public Proposal addMilestones(Long proposalId, List<ProposalMilestone> milestones) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new EntityNotFoundException("Proposal not found with id: " + proposalId));

        if (!MILESTONE_ALLOWED_STATUSES.contains(proposal.getStatus())) {
            throw new IllegalArgumentException("Milestones can only be added to SUBMITTED or SHORTLISTED proposals");
        }
        if (milestones == null || milestones.isEmpty()) {
            throw new IllegalArgumentException("At least one milestone is required");
        }

        List<ProposalMilestone> existingMilestones = proposal.getProposalMilestones();
        double existingTotal = existingMilestones == null
                ? 0.0
                : existingMilestones.stream()
                        .map(ProposalMilestone::getAmount)
                        .filter(amount -> amount != null)
                        .mapToDouble(Double::doubleValue)
                        .sum();
        double newTotal = milestones.stream()
                .peek(this::validateMilestone)
                .mapToDouble(ProposalMilestone::getAmount)
                .sum();

        if (proposal.getBidAmount() == null || existingTotal + newTotal > proposal.getBidAmount()) {
            throw new IllegalArgumentException("Total milestone amounts cannot exceed proposal bidAmount");
        }

        int nextOrder = existingMilestones == null
                ? 1
                : existingMilestones.stream()
                        .map(ProposalMilestone::getMilestoneOrder)
                        .filter(order -> order != null)
                        .max(Integer::compareTo)
                        .orElse(0) + 1;

        for (ProposalMilestone milestone : milestones) {
            milestone.setId(null);
            milestone.setMilestoneOrder(nextOrder++);
            milestone.setStatus(MilestoneStatus.PENDING);
            proposal.addProposalMilestone(milestone);
        }

        Proposal savedProposal = proposalRepository.save(proposal);
        if (savedProposal.getProposalMilestones() != null) {
            savedProposal.getProposalMilestones().sort(Comparator.comparing(ProposalMilestone::getMilestoneOrder));
        }
        return savedProposal;
    }

    private void validateMilestone(ProposalMilestone milestone) {
        if (milestone == null) {
            throw new IllegalArgumentException("Milestone is required");
        }
        if (milestone.getTitle() == null || milestone.getTitle().isBlank()) {
            throw new IllegalArgumentException("Milestone title is required");
        }
        if (milestone.getDescription() == null || milestone.getDescription().isBlank()) {
            throw new IllegalArgumentException("Milestone description is required");
        }
        if (milestone.getAmount() == null || milestone.getAmount() <= 0) {
            throw new IllegalArgumentException("Milestone amount must be positive");
        }
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
