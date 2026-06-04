package com.team01.freelance.proposal.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.team01.freelance.common.observer.EventSubject;
import com.team01.freelance.proposal.graph.InteractionGraphService;
import com.team01.freelance.proposal.model.Proposal;
import com.team01.freelance.proposal.model.ProposalStatus;
import com.team01.freelance.proposal.repository.ProposalRepository;

@ExtendWith(MockitoExtension.class)
class ProposalServiceObserverTest {

    @Mock
    private ProposalRepository proposalRepository;

    @Mock
    private EventSubject proposalEventSubject;

    @Mock
    private InteractionGraphService interactionGraphService;

    @InjectMocks
    private ProposalService proposalService;

    private Proposal proposal;

    @BeforeEach
    void setUp() {
        proposal = new Proposal();
        proposal.setId(10L);
        proposal.setJobId(20L);
        proposal.setFreelancerId(30L);
        proposal.setStatus(ProposalStatus.SUBMITTED);
    }

    @Test
    void deleteProposalById_notifiesProposalDeleted() {
        org.mockito.Mockito.when(proposalRepository.findById(10L)).thenReturn(java.util.Optional.of(proposal));

        proposalService.deleteProposalById(10L);

        ArgumentCaptor<Map<String, Object>> payload = ArgumentCaptor.forClass(Map.class);
        verify(proposalEventSubject).notifyObservers(eq("PROPOSAL_DELETED"), payload.capture());
        org.junit.jupiter.api.Assertions.assertEquals(10L, payload.getValue().get("proposalId"));
        org.junit.jupiter.api.Assertions.assertEquals("PROPOSAL_DELETED", payload.getValue().get("action"));
    }
}
