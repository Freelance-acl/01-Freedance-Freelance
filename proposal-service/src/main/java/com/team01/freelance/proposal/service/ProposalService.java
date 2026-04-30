package com.team01.freelance.proposal.service;

import com.team01.freelance.proposal.model.Proposal;
import com.team01.freelance.proposal.repository.ProposalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProposalService {

    @Autowired
    private ProposalRepository proposalRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Proposal> getAllProposals() {
        return proposalRepository.findAll();
    }

    public Optional<Proposal> getProposalById(Long id) {
        return proposalRepository.findById(id);
    }

    public Proposal createProposal(Proposal proposal) {
        validateProposalReferences(proposal.getFreelancerId(), proposal.getJobId());
        return proposalRepository.save(proposal);
    }

    /**
     * Updates an existing proposal and throws if it does not exist.
     *
     * @param id The ID of the proposal to update
     * @param proposalDetails The object containing updated fields
     * @return The updated proposal
     * @throws RuntimeException if the proposal is not found
     */
    public Proposal updateProposal(Long id, Proposal proposalDetails) {
        return proposalRepository.findById(id).map(existingProposal -> {
                existingProposal.setJobId(proposalDetails.getJobId());
                existingProposal.setFreelancerId(proposalDetails.getFreelancerId());
                existingProposal.setCoverLetter(proposalDetails.getCoverLetter());
                existingProposal.setBidAmount(proposalDetails.getBidAmount());
                existingProposal.setEstimatedDays(proposalDetails.getEstimatedDays());
                existingProposal.setStatus(proposalDetails.getStatus());
                existingProposal.setMetadata(proposalDetails.getMetadata());
                existingProposal.setAcceptedAt(proposalDetails.getAcceptedAt());
            validateProposalReferences(existingProposal.getFreelancerId(), existingProposal.getJobId());
            return proposalRepository.save(existingProposal);
        }).orElseThrow(() -> new RuntimeException("Proposal not found with id: " + id));
    }

    private void validateProposalReferences(Long freelancerId, Long jobId) {
        validateReferenceExists("users", freelancerId, "Freelancer");
        validateReferenceExists("jobs", jobId, "Job");
    }

    private void validateReferenceExists(String tableName, Long id, String label) {
        if (id == null) {
            throw new IllegalArgumentException(label + " id is required");
        }

        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM " + tableName + " WHERE id = ?)",
                Boolean.class,
                id
        );

        if (!Boolean.TRUE.equals(exists)) {
            throw new IllegalArgumentException(label + " not found with id: " + id);
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
