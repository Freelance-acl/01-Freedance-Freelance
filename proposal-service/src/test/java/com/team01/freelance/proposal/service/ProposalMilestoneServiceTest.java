package com.team01.freelance.proposal.service;

import com.team01.freelance.proposal.model.MilestoneStatus;
import com.team01.freelance.proposal.model.ProposalMilestone;
import com.team01.freelance.proposal.repository.ProposalMilestoneRepository;
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

        Optional<ProposalMilestone> result = proposalMilestoneService.updateProposalMilestone(id, updatePayload);

        assertTrue(result.isPresent());
        ProposalMilestone updated = result.get();
        assertEquals("New Title", updated.getTitle());
        assertEquals("Old Description", updated.getDescription(), "Description should remain unchanged");
        assertEquals(100.0, updated.getAmount(), "Amount should remain unchanged");
        assertEquals(MilestoneStatus.PENDING, updated.getStatus(), "Status should remain unchanged");
    }

    @Test
    void updateProposalMilestoneReturnsEmptyIfNotFound() {
        Long id = 1L;
        when(proposalMilestoneRepository.findById(id)).thenReturn(Optional.empty());

        Optional<ProposalMilestone> result = proposalMilestoneService.updateProposalMilestone(id, new ProposalMilestone());

        assertFalse(result.isPresent());
    }
}
