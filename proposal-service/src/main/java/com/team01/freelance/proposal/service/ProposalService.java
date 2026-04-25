package com.team01.freelance.proposal.service;

import com.team01.freelance.proposal.model.Proposal;
import com.team01.freelance.proposal.repository.ProposalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProposalService {

    @Autowired
    private ProposalRepository proposalRepository;

    public List<Proposal> getAllProposals() {
        return proposalRepository.findAll();
    }

    public Optional<Proposal> getProposalById(Long id) {
        return proposalRepository.findById(id);
    }

    public Proposal createProposal(Proposal proposal) {
        return proposalRepository.save(proposal);
    }

    public Optional<Proposal> updateProposal(Long id, Proposal proposalDetails) {
        return proposalRepository.findById(id).map(existingProposal -> {
            if (proposalDetails.getJobId() != null) {
                existingProposal.setJobId(proposalDetails.getJobId());
            }
            if (proposalDetails.getFreelancerId() != null) {
                existingProposal.setFreelancerId(proposalDetails.getFreelancerId());
            }
            if (proposalDetails.getCoverLetter() != null) {
                existingProposal.setCoverLetter(proposalDetails.getCoverLetter());
            }
            if (proposalDetails.getBidAmount() != null) {
                existingProposal.setBidAmount(proposalDetails.getBidAmount());
            }
            if (proposalDetails.getEstimatedDays() != null) {
                existingProposal.setEstimatedDays(proposalDetails.getEstimatedDays());
            }
            if (proposalDetails.getStatus() != null) {
                existingProposal.setStatus(proposalDetails.getStatus());
            }
            if (proposalDetails.getMetadata() != null) {
                existingProposal.setMetadata(proposalDetails.getMetadata());
            }
            if (proposalDetails.getAcceptedAt() != null) {
                existingProposal.setAcceptedAt(proposalDetails.getAcceptedAt());
            }
            return proposalRepository.save(existingProposal);
        });
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
