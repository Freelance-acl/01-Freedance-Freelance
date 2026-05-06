package com.team01.freelance.proposal.service;

import com.team01.freelance.proposal.model.Proposal;
import com.team01.freelance.proposal.repository.ProposalRepository;
import com.team01.freelance.job.repository.JobRepository;
import com.team01.freelance.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
}
