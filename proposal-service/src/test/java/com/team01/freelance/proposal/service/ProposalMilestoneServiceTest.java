package com.team01.freelance.proposal.service;

import com.team01.freelance.proposal.model.MilestoneStatus;
import com.team01.freelance.proposal.model.Proposal;
import com.team01.freelance.proposal.model.ProposalMilestone;
import com.team01.freelance.proposal.repository.ProposalMilestoneRepository;
import com.team01.freelance.proposal.repository.ProposalRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProposalMilestoneServiceTest {

    @Mock
    private ProposalMilestoneRepository proposalMilestoneRepository;

    @Mock
    private ProposalRepository proposalRepository;

    @InjectMocks
    private ProposalMilestoneService proposalMilestoneService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void updateProposalMilestoneMergesNonNullFields() {
        Long id = 1L;
        ProposalMilestone existing = new ProposalMilestone();
        existing.setId(id);
        existing.setTitle("Old Title");
        existing.setDescription("Old Description");
        existing.setAmount(100.0);
        existing.setStatus(MilestoneStatus.PENDING);

        ProposalMilestone updatePayload = new ProposalMilestone();
        updatePayload.setTitle("New Title");
        // description, amount, status are null in the payload

        when(proposalMilestoneRepository.findById(id)).thenReturn(Optional.of(existing));
        when(proposalMilestoneRepository.save(any(ProposalMilestone.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProposalMilestone result = proposalMilestoneService.updateProposalMilestone(id, updatePayload);

        assertNotNull(result);
        ProposalMilestone updated = result;
        assertEquals("New Title", updated.getTitle());
        assertEquals("Old Description", updated.getDescription(), "Description should remain unchanged");
        assertEquals(100.0, updated.getAmount(), "Amount should remain unchanged");
        assertEquals(MilestoneStatus.PENDING, updated.getStatus(), "Status should remain unchanged");
    }

    @Test
    void updateProposalMilestoneThrowsIfNotFound() {
        Long id = 1L;
        when(proposalMilestoneRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> proposalMilestoneService.updateProposalMilestone(id, new ProposalMilestone()));
    }

    @Test
    void updateProposalMilestone_ShouldValidateProposalIfProvided() {
        // Arrange
        Long id = 1L;
        ProposalMilestone existing = new ProposalMilestone();
        existing.setId(id);
        
        ProposalMilestone incoming = new ProposalMilestone();
        Proposal proposal = new Proposal();
        proposal.setId(50L);
        incoming.setProposal(proposal);

        when(proposalMilestoneRepository.findById(id)).thenReturn(Optional.of(existing));
        when(proposalRepository.findById(50L)).thenReturn(Optional.of(proposal));
        when(proposalMilestoneRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Act
        ProposalMilestone result = proposalMilestoneService.updateProposalMilestone(id, incoming);

        // Assert
        assertNotNull(result);
        verify(proposalRepository).findById(50L);
    }

    @Test
    void updateProposalMilestone_ShouldThrowIfProposalNotFound() {
        // Arrange
        Long id = 1L;
        ProposalMilestone existing = new ProposalMilestone();
        existing.setId(id);
        
        ProposalMilestone incoming = new ProposalMilestone();
        Proposal proposal = new Proposal();
        proposal.setId(50L);
        incoming.setProposal(proposal);

        when(proposalMilestoneRepository.findById(id)).thenReturn(Optional.of(existing));
        when(proposalRepository.findById(50L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> proposalMilestoneService.updateProposalMilestone(id, incoming));
    }

    @Test
    void createProposalMilestone_ShouldValidateProposal() {
        // Arrange
        ProposalMilestone milestone = new ProposalMilestone();
        Proposal proposal = new Proposal();
        proposal.setId(50L);
        milestone.setProposal(proposal);

        when(proposalRepository.findById(50L)).thenReturn(Optional.of(proposal));
        when(proposalMilestoneRepository.save(milestone)).thenReturn(milestone);

        // Act
        ProposalMilestone result = proposalMilestoneService.createProposalMilestone(milestone);

        // Assert
        assertNotNull(result);
        verify(proposalRepository).findById(50L);
    }

    @Test
    void createProposalMilestone_ShouldThrowIfProposalNotFound() {
        // Arrange
        ProposalMilestone milestone = new ProposalMilestone();
        Proposal proposal = new Proposal();
        proposal.setId(50L);
        milestone.setProposal(proposal);

        when(proposalRepository.findById(50L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> proposalMilestoneService.createProposalMilestone(milestone));
    }
}
