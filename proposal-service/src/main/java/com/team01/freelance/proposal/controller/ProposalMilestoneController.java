package com.team01.freelance.proposal.controller;

import com.team01.freelance.proposal.model.ProposalMilestone;
import com.team01.freelance.proposal.service.ProposalMilestoneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/proposal-milestones")
public class ProposalMilestoneController {

    @Autowired
    private ProposalMilestoneService proposalMilestoneService;

    @GetMapping
    public ResponseEntity<List<ProposalMilestone>> getAllProposalMilestones() {
        return ResponseEntity.ok(proposalMilestoneService.getAllProposalMilestones());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProposalMilestone> getProposalMilestoneById(@PathVariable Long id) {
        return proposalMilestoneService.getProposalMilestoneById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ProposalMilestone> createProposalMilestone(@RequestBody ProposalMilestone proposalMilestone) {
        return ResponseEntity.ok(proposalMilestoneService.createProposalMilestone(proposalMilestone));
    }

    /**
     * Updates a proposal milestone by ID.
     *
     * @param id the proposal milestone ID
     * @param proposalMilestone the update payload
     * @return 200 with updated proposal milestone, or 404 if not found
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProposalMilestone> updateProposalMilestone(@PathVariable Long id, @RequestBody ProposalMilestone proposalMilestone) {
        try {
            return ResponseEntity.ok(proposalMilestoneService.updateProposalMilestone(id, proposalMilestone));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProposalMilestoneById(@PathVariable Long id) {
        if (proposalMilestoneService.deleteProposalMilestoneById(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/all")
    public ResponseEntity<Void> deleteAllProposalMilestones() {
        proposalMilestoneService.deleteAllProposalMilestones();
        return ResponseEntity.noContent().build();
    }
}
