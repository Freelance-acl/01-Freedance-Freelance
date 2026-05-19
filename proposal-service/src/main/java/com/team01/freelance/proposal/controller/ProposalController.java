package com.team01.freelance.proposal.controller;

import com.team01.freelance.proposal.dto.FeeEstimateDTO;
import com.team01.freelance.proposal.dto.FeeEstimateRequest;
import com.team01.freelance.proposal.dto.ProposalDetailsDTO;
import com.team01.freelance.proposal.dto.ProposalAnalyticsDTO;
import com.team01.freelance.proposal.model.Proposal;
import com.team01.freelance.proposal.model.ProposalMilestone;
import com.team01.freelance.proposal.service.ProposalService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/proposals")
public class ProposalController {

    @Autowired
    private ProposalService proposalService;

    @GetMapping
    public ResponseEntity<List<Proposal>> getAllProposals() {
        return ResponseEntity.ok(proposalService.getAllProposals());
    }

    /**
     * Search proposals by optional status and inclusive date range on {@code submittedAt}, newest first.
     */
    @GetMapping("/search")
    public ResponseEntity<List<Proposal>> searchProposals(
            @RequestParam(required = false) String status,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(proposalService.searchProposals(status, startDate, endDate));
    }

    /**
     * [S3-F5] Filter proposals by a metadata key/value pair (JSONB equality).
     */
    @GetMapping("/metadata/search")
    public ResponseEntity<List<Proposal>> searchProposalsByMetadata(
            @RequestParam String key,
            @RequestParam String value) {
        try {
            return ResponseEntity.ok(proposalService.searchProposalsByMetadata(key, value));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/analytics")
    public ResponseEntity<ProposalAnalyticsDTO> getProposalAnalytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(proposalService.getProposalAnalytics(startDate, endDate));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Proposal> getProposalById(@PathVariable Long id) {
        return proposalService.getProposalById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/estimate")
    public ResponseEntity<FeeEstimateDTO> estimatePlatformFee(@RequestBody FeeEstimateRequest request) {
        try {
            return ResponseEntity.ok(proposalService.estimatePlatformFee(
                    request.getBidAmount(),
                    request.getEstimatedDays()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{proposalId}/details")
    public ResponseEntity<ProposalDetailsDTO> getProposalDetails(@PathVariable Long proposalId) {
        try {
            return ResponseEntity.ok(proposalService.getProposalDetails(proposalId));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<Proposal> createProposal(@RequestBody Proposal proposal) {
        try {
            return ResponseEntity.ok(proposalService.createProposal(proposal));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } 
    }

    /**
     * Updates a proposal by ID.
     *
     * @param id the proposal ID
     * @param proposal the update payload
     * @return 200 with updated proposal, or 404 if not found
     */
    @PutMapping("/{id}")
    public ResponseEntity<Proposal> updateProposal(@PathVariable Long id, @RequestBody Proposal proposal) {
        try {
            return ResponseEntity.ok(proposalService.updateProposal(id, proposal));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/accept")
    public ResponseEntity<Proposal> acceptProposal(@PathVariable("id") Long proposalId) {
        try {
            return ResponseEntity.ok(proposalService.acceptProposal(proposalId));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{proposalId}/milestones")
    public ResponseEntity<Proposal> addMilestones(
            @PathVariable Long proposalId,
            @RequestBody List<ProposalMilestone> milestones) {
        try {
            return ResponseEntity.ok(proposalService.addMilestones(proposalId, milestones));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/withdraw")
    public ResponseEntity<Proposal> withdrawProposal(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(proposalService.withdrawProposal(id));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<Proposal> completeProposal(@PathVariable("id") Long proposalId) {
        try {
            return ResponseEntity.ok(proposalService.completeProposal(proposalId));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProposalById(@PathVariable Long id) {
        if (proposalService.deleteProposalById(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/all")
    public ResponseEntity<Void> deleteAllProposals() {
        proposalService.deleteAllProposals();
        return ResponseEntity.noContent().build();
    }
}
