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
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [S3-F6] Integration tests for {@code GET /api/proposals/analytics}.
 */
class ProposalAnalyticsIntegrationTest extends AbstractIntegrationTest {

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
    void marchAnalytics_matchesMilestoneSpecScenario() throws Exception {
        LocalDateTime march = LocalDateTime.of(2026, 3, 15, 12, 0);

        saveProposal(ProposalStatus.ACCEPTED, 500.0, march);
        saveProposal(ProposalStatus.ACCEPTED, 1000.0, march);
        saveProposal(ProposalStatus.ACCEPTED, 1500.0, march);
        saveProposal(ProposalStatus.ACCEPTED, 2000.0, march);
        saveProposal(ProposalStatus.REJECTED, 300.0, march);
        saveProposal(ProposalStatus.REJECTED, 300.0, march);
        saveProposal(ProposalStatus.REJECTED, 300.0, march);
        saveProposal(ProposalStatus.SUBMITTED, 400.0, march);
        saveProposal(ProposalStatus.SUBMITTED, 400.0, march);
        saveProposal(ProposalStatus.SUBMITTED, 400.0, march);

        mockMvc.perform(get("/api/proposals/analytics")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProposals").value(10))
                .andExpect(jsonPath("$.acceptedProposals").value(4))
                .andExpect(jsonPath("$.rejectedProposals").value(3))
                .andExpect(jsonPath("$.totalBidValue").value(7100.0))
                .andExpect(jsonPath("$.averageBid").value(710.0))
                .andExpect(jsonPath("$.acceptanceRate").value(40.0));
    }

    @Test
    void analytics_rejectsStartAfterEnd() throws Exception {
        mockMvc.perform(get("/api/proposals/analytics")
                        .param("startDate", "2026-04-10")
                        .param("endDate", "2026-04-01"))
                .andExpect(status().isBadRequest());
    }

    private void saveProposal(ProposalStatus status, double bidAmount, LocalDateTime submittedAt) {
        Proposal proposal = new Proposal();
        proposal.setJobId(ProposalTestData.JOB_ID);
        proposal.setFreelancerId(ProposalTestData.FREELANCER_ID);
        proposal.setCoverLetter("Analytics letter");
        proposal.setBidAmount(bidAmount);
        proposal.setEstimatedDays(7);
        proposal.setStatus(status);
        proposal.setSubmittedAt(submittedAt);
        proposalRepository.save(proposal);
    }
}
