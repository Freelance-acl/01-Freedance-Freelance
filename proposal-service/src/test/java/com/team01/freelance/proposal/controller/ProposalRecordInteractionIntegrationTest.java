package com.team01.freelance.proposal.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
 * [S3-F11] Integration tests for {@code POST /api/proposals/{proposalId}/record-interaction}.
 */
class ProposalRecordInteractionIntegrationTest extends FeignIntegrationTestSupport {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ProposalRepository proposalRepository;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(webApplicationContext);
        proposalRepository.deleteAll();
        when(userServiceClient.getUser(ProposalTestData.FREELANCER_ID))
                .thenReturn(FeignTestFixtures.activeFreelancer(ProposalTestData.FREELANCER_ID));
        when(jobServiceClient.getJob(ProposalTestData.JOB_ID))
                .thenReturn(FeignTestFixtures.openJob(ProposalTestData.JOB_ID, ProposalTestData.CLIENT_ID));
    }

    @Test
    void recordInteraction_submittedProposal_returns200() throws Exception {
        Proposal proposal = ProposalTestData.saveProposal(proposalRepository, ProposalStatus.SUBMITTED, 500.0);

        mockMvc.perform(post("/api/proposals/{proposalId}/record-interaction", proposal.getId()))
                .andExpect(status().isOk());
    }

    @Test
    void recordInteraction_isIdempotent_secondCallStillReturns200() throws Exception {
        Proposal proposal = ProposalTestData.saveProposal(proposalRepository, ProposalStatus.SUBMITTED, 500.0);

        mockMvc.perform(post("/api/proposals/{proposalId}/record-interaction", proposal.getId()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/proposals/{proposalId}/record-interaction", proposal.getId()))
                .andExpect(status().isOk());
    }

    @Test
    void recordInteraction_nonSubmittedProposal_returns400() throws Exception {
        Proposal proposal = ProposalTestData.saveProposal(proposalRepository, ProposalStatus.ACCEPTED, 500.0);

        mockMvc.perform(post("/api/proposals/{proposalId}/record-interaction", proposal.getId()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recordInteraction_nonExistentProposal_returns404() throws Exception {
        mockMvc.perform(post("/api/proposals/{proposalId}/record-interaction", 999999L))
                .andExpect(status().isNotFound());
    }
}
