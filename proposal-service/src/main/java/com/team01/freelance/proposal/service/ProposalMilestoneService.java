package com.team01.freelance.proposal.service;

import com.team01.freelance.proposal.model.ProposalMilestone;
import com.team01.freelance.proposal.repository.ProposalMilestoneRepository;
import com.team01.freelance.proposal.repository.ProposalRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProposalMilestoneService {

    @Autowired
    private ProposalMilestoneRepository proposalMilestoneRepository;

    @Autowired
    private ProposalRepository proposalRepository;

    public List<ProposalMilestone> getAllProposalMilestones() {
        return proposalMilestoneRepository.findAll();
    }

    public Optional<ProposalMilestone> getProposalMilestoneById(Long id) {
        return proposalMilestoneRepository.findById(id);
    }

    public ProposalMilestone createProposalMilestone(ProposalMilestone proposalMilestone) {
        if (proposalMilestone.getProposal() != null && proposalMilestone.getProposal().getId() != null) {
            proposalMilestone.setProposal(proposalRepository.findById(proposalMilestone.getProposal().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Proposal not found with id: " + proposalMilestone.getProposal().getId())));
        }
        return proposalMilestoneRepository.save(proposalMilestone);
    }

    /**
     * Updates an existing proposal milestone and throws if it does not exist.
     * Validates the existence of the associated proposal if provided.
     *
     * @param id The ID of the proposal milestone to update
     * @param proposalMilestone The object containing updated fields
     * @return The updated proposal milestone
     * @throws EntityNotFoundException if the proposal milestone or associated proposal is not found
     */
    public ProposalMilestone updateProposalMilestone(Long id, ProposalMilestone proposalMilestone) {
        return proposalMilestoneRepository.findById(id).map(existing -> {
                if (proposalMilestone.getMilestoneOrder() != null) existing.setMilestoneOrder(proposalMilestone.getMilestoneOrder());
                if (proposalMilestone.getTitle() != null) existing.setTitle(proposalMilestone.getTitle());
                if (proposalMilestone.getDescription() != null) existing.setDescription(proposalMilestone.getDescription());
                if (proposalMilestone.getAmount() != null) existing.setAmount(proposalMilestone.getAmount());
                if (proposalMilestone.getStatus() != null) existing.setStatus(proposalMilestone.getStatus());
                if (proposalMilestone.getMetadata() != null) existing.setMetadata(proposalMilestone.getMetadata());
                if (proposalMilestone.getProposal() != null && proposalMilestone.getProposal().getId() != null) {
                    existing.setProposal(proposalRepository.findById(proposalMilestone.getProposal().getId())
                            .orElseThrow(() -> new EntityNotFoundException("Proposal not found with id: " + proposalMilestone.getProposal().getId())));
                }
            return proposalMilestoneRepository.save(existing);
        }).orElseThrow(() -> new EntityNotFoundException("Proposal Milestone not found with id: " + id));
    }

    public boolean deleteProposalMilestoneById(Long id) {
        if (!proposalMilestoneRepository.existsById(id)) {
            return false;
        }
        proposalMilestoneRepository.deleteById(id);
        return true;
    }

    public void deleteAllProposalMilestones() {
        proposalMilestoneRepository.deleteAll();
    }
}
