package com.team01.freelance.proposal.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team01.freelance.proposal.model.Proposal;
import com.team01.freelance.proposal.model.ProposalStatus;
import com.team01.freelance.proposal.repository.ProposalRepository;
import com.team01.freelance.proposal.support.FeignIntegrationTestSupport;
import com.team01.freelance.proposal.support.FeignTestFixtures;
import com.team01.freelance.proposal.support.ProposalTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

/**
 * [S3-F2] Integration tests for {@code PUT /api/proposals/{proposalId}/accept}.
 */
class ProposalAcceptIntegrationTest extends FeignIntegrationTestSupport {

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
    void acceptSubmittedProposal_publishesEventWithoutDirectJobOrContractWrites() throws Exception {
        Proposal proposal = ProposalTestData.saveProposal(proposalRepository, ProposalStatus.SUBMITTED, 2000.0);
        when(userServiceClient.getUser(ProposalTestData.FREELANCER_ID))
                .thenReturn(FeignTestFixtures.activeFreelancer(ProposalTestData.FREELANCER_ID));

        mockMvc.perform(put("/api/proposals/{id}/accept", proposal.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.acceptedAt").exists());

        Proposal updated = proposalRepository.findById(proposal.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ProposalStatus.ACCEPTED);
        assertThat(updated.getAcceptedAt()).isNotNull();
    }

    @Test
    void acceptAlreadyAcceptedProposal_returns400() throws Exception {
        Proposal proposal = ProposalTestData.saveProposal(proposalRepository, ProposalStatus.ACCEPTED, 2000.0);

        mockMvc.perform(put("/api/proposals/{id}/accept", proposal.getId()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void acceptProposalForNonFreelancer_returns400() throws Exception {
        Proposal proposal = ProposalTestData.saveProposal(
                proposalRepository, ProposalTestData.JOB_ID, ProposalTestData.CLIENT_ID,
                ProposalStatus.SUBMITTED, 2000.0);
        when(userServiceClient.getUser(ProposalTestData.CLIENT_ID))
                .thenReturn(FeignTestFixtures.activeClient(ProposalTestData.CLIENT_ID));

        mockMvc.perform(put("/api/proposals/{id}/accept", proposal.getId()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void acceptUnknownProposal_returns404() throws Exception {
        mockMvc.perform(put("/api/proposals/{id}/accept", 999_999L))
                .andExpect(status().isNotFound());
    }
}
