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
        if (!proposalMilestoneRepository.existsById(id)) {
            return Optional.empty();
        }
        proposalMilestone.setId(id);
        return Optional.of(proposalMilestoneRepository.save(proposalMilestone));
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
