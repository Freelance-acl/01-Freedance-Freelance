package com.team01.freelance.proposal.controller;

import com.team01.freelance.proposal.model.Proposal;
import com.team01.freelance.proposal.model.ProposalStatus;
import com.team01.freelance.proposal.repository.ProposalRepository;
import com.team01.freelance.proposal.support.AbstractIntegrationTest;
import com.team01.freelance.proposal.support.ProposalTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [S3-F10] Integration tests for {@code GET /api/proposals/analytics/dashboard}.
 */
class ProposalAnalyticsDashboardIntegrationTest extends AbstractIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ProposalRepository proposalRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(webApplicationContext);

        var cache = cacheManager.getCache("S3-F10");
        if (cache != null) {
            cache.clear();
        }

        proposalRepository.deleteAll();
    }

    @Test
    void dashboard_aggregatesProposalsByStatus() throws Exception {
        LocalDateTime march = LocalDateTime.of(2026, 3, 15, 12, 0);

        saveProposal(ProposalStatus.ACCEPTED, 1000.0, 10, march);
        saveProposal(ProposalStatus.ACCEPTED, 2000.0, 20, march);
        saveProposal(ProposalStatus.REJECTED, 500.0, 5, march);
        saveProposal(ProposalStatus.SUBMITTED, 750.0, 15, march);

        mockMvc.perform(get("/api/proposals/analytics/dashboard")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProposals").value(4))
                .andExpect(jsonPath("$.acceptanceRate").value(50.0))
                .andExpect(jsonPath("$.averageBidAmount").value(1062.5))
                .andExpect(jsonPath("$.averageEstimatedDays").value(12.5))
                .andExpect(jsonPath("$.proposalsByStatus.ACCEPTED").value(2))
                .andExpect(jsonPath("$.proposalsByStatus.REJECTED").value(1))
                .andExpect(jsonPath("$.proposalsByStatus.SUBMITTED").value(1));
    }

    @Test
    void dashboard_emptyRange_returnsZeros() throws Exception {
        mockMvc.perform(get("/api/proposals/analytics/dashboard")
                        .param("startDate", "2020-01-01")
                        .param("endDate", "2020-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProposals").value(0))
                .andExpect(jsonPath("$.acceptanceRate").value(0.0))
                .andExpect(jsonPath("$.averageBidAmount").value(0.0));
    }

    @Test
    void dashboard_invalidDateRange_returns400() throws Exception {
        mockMvc.perform(get("/api/proposals/analytics/dashboard")
                        .param("startDate", "2026-04-10")
                        .param("endDate", "2026-04-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void dashboard_excludesProposalsOutsideRange() throws Exception {
        LocalDateTime inRange = LocalDateTime.of(2026, 3, 15, 12, 0);
        LocalDateTime outOfRange = LocalDateTime.of(2026, 5, 1, 12, 0);

        saveProposal(ProposalStatus.SUBMITTED, 500.0, 7, inRange);
        saveProposal(ProposalStatus.SUBMITTED, 999.0, 7, outOfRange);

        mockMvc.perform(get("/api/proposals/analytics/dashboard")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProposals").value(1));
    }

    private void saveProposal(ProposalStatus status, double bidAmount, int estimatedDays, LocalDateTime submittedAt) {
        Proposal proposal = new Proposal();
        proposal.setJobId(ProposalTestData.JOB_ID);
        proposal.setFreelancerId(ProposalTestData.FREELANCER_ID);
        proposal.setCoverLetter("Cover letter");
        proposal.setBidAmount(bidAmount);
        proposal.setEstimatedDays(estimatedDays);
        proposal.setStatus(status);
        proposal.setSubmittedAt(submittedAt);
        proposalRepository.save(proposal);
    }
}
