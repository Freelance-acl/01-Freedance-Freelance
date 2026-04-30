package com.team01.freelance.proposal.service;

import com.team01.freelance.proposal.model.ProposalMilestone;
import com.team01.freelance.proposal.repository.ProposalMilestoneRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProposalMilestoneService {

    @Autowired
    private ProposalMilestoneRepository proposalMilestoneRepository;

    public List<ProposalMilestone> getAllProposalMilestones() {
        return proposalMilestoneRepository.findAll();
    }

    public Optional<ProposalMilestone> getProposalMilestoneById(Long id) {
        return proposalMilestoneRepository.findById(id);
    }

    public ProposalMilestone createProposalMilestone(ProposalMilestone proposalMilestone) {
        return proposalMilestoneRepository.save(proposalMilestone);
    }

    public Optional<ProposalMilestone> updateProposalMilestone(Long id, ProposalMilestone proposalMilestone) {
        return proposalMilestoneRepository.findById(id).map(existing -> {
            if (proposalMilestone.getMilestoneOrder() != null) {
                existing.setMilestoneOrder(proposalMilestone.getMilestoneOrder());
            }
            if (proposalMilestone.getTitle() != null) {
                existing.setTitle(proposalMilestone.getTitle());
            }
            if (proposalMilestone.getDescription() != null) {
                existing.setDescription(proposalMilestone.getDescription());
            }
            if (proposalMilestone.getAmount() != null) {
                existing.setAmount(proposalMilestone.getAmount());
            }
            if (proposalMilestone.getStatus() != null) {
                existing.setStatus(proposalMilestone.getStatus());
            }
            if (proposalMilestone.getMetadata() != null) {
                existing.setMetadata(proposalMilestone.getMetadata());
            }
            if (proposalMilestone.getProposal() != null) {
                existing.setProposal(proposalMilestone.getProposal());
            }
            return proposalMilestoneRepository.save(existing);
        });
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
