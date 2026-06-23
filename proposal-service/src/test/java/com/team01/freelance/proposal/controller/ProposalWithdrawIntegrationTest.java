package com.team01.freelance.proposal.controller;

import com.team01.freelance.proposal.model.Proposal;
import com.team01.freelance.proposal.model.ProposalStatus;
import com.team01.freelance.proposal.repository.ProposalRepository;
import com.team01.freelance.proposal.support.AbstractIntegrationTest;
import com.team01.freelance.proposal.support.ProposalTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [S3-F7] Integration tests for {@code PUT /api/proposals/{id}/withdraw}.
 */
@Transactional
class ProposalWithdrawIntegrationTest extends AbstractIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ProposalRepository proposalRepository;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(webApplicationContext);
        proposalRepository.deleteAll();
    }

    @Test
    void withdrawSubmittedProposalSetsProposalWithdrawn() throws Exception {
        Proposal proposal = ProposalTestData.saveProposal(proposalRepository, ProposalStatus.SUBMITTED, 250.0);

        mockMvc.perform(put("/api/proposals/{id}/withdraw", proposal.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WITHDRAWN"));

        assertThat(proposalRepository.findById(proposal.getId()).orElseThrow().getStatus())
                .isEqualTo(ProposalStatus.WITHDRAWN);
    }

    @Test
    void withdrawOnlyActiveProposalForInProgressJobDoesNotDirectlyReopenJob() throws Exception {
        Proposal proposal = ProposalTestData.saveProposal(proposalRepository, ProposalStatus.SUBMITTED, 250.0);

        mockMvc.perform(put("/api/proposals/{id}/withdraw", proposal.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WITHDRAWN"));
    }

    @Test
    void withdrawAcceptedProposalReturnsBadRequest() throws Exception {
        Proposal proposal = ProposalTestData.saveProposal(proposalRepository, ProposalStatus.ACCEPTED, 250.0);

        mockMvc.perform(put("/api/proposals/{id}/withdraw", proposal.getId()))
                .andExpect(status().isBadRequest());

        assertThat(proposalRepository.findById(proposal.getId()).orElseThrow().getStatus())
                .isEqualTo(ProposalStatus.ACCEPTED);
    }

    @Test
    void withdrawRejectedProposalReturnsBadRequest() throws Exception {
        Proposal proposal = ProposalTestData.saveProposal(proposalRepository, ProposalStatus.REJECTED, 250.0);

        mockMvc.perform(put("/api/proposals/{id}/withdraw", proposal.getId()))
                .andExpect(status().isBadRequest());

        assertThat(proposalRepository.findById(proposal.getId()).orElseThrow().getStatus())
                .isEqualTo(ProposalStatus.REJECTED);
    }

    @Test
    void withdrawMissingProposalReturnsNotFound() throws Exception {
        mockMvc.perform(put("/api/proposals/{id}/withdraw", 999999L))
                .andExpect(status().isNotFound());
    }
}
