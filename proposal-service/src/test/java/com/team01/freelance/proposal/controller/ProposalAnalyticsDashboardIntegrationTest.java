package com.team01.freelance.proposal.controller;

import com.team01.freelance.job.model.Job;
import com.team01.freelance.job.model.JobCategory;
import com.team01.freelance.job.model.JobStatus;
import com.team01.freelance.job.repository.JobRepository;
import com.team01.freelance.proposal.model.Proposal;
import com.team01.freelance.proposal.model.ProposalStatus;
import com.team01.freelance.proposal.repository.ProposalRepository;
import com.team01.freelance.proposal.support.AbstractIntegrationTest;
import com.team01.freelance.user.model.User;
import com.team01.freelance.user.model.UserRole;
import com.team01.freelance.user.model.UserStatus;
import com.team01.freelance.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithAnonymousUser;
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
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

    private Job job;
    private User freelancer;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(webApplicationContext);

        proposalRepository.deleteAll();
        jobRepository.deleteAll();
        userRepository.deleteAll();

        job = new Job();
        job.setClientId(1L);
        job.setTitle("Dashboard job");
        job.setDescription("Desc");
        job.setCategory(JobCategory.WEB_DEV);
        job.setStatus(JobStatus.OPEN);
        job.setBudgetMin(100.0);
        job.setBudgetMax(10000.0);
        job = jobRepository.save(job);

        freelancer = new User();
        freelancer.setName("Dashboard Freelancer");
        freelancer.setEmail("dashboard-" + System.nanoTime() + "@test.dev");
        freelancer.setPassword("secret");
        freelancer.setPhone("+7000" + (System.nanoTime() % 1_000_000_000L));
        freelancer.setRole(UserRole.FREELANCER);
        freelancer.setStatus(UserStatus.ACTIVE);
        freelancer = userRepository.save(freelancer);
    }

    @Test
    void marchDashboard_matchesMilestoneSpecScenario() throws Exception {
        LocalDateTime march = LocalDateTime.of(2026, 3, 15, 12, 0);

        saveProposal(ProposalStatus.ACCEPTED, 500.0, 10, march);
        saveProposal(ProposalStatus.ACCEPTED, 1000.0, 10, march);
        saveProposal(ProposalStatus.ACCEPTED, 1500.0, 10, march);
        saveProposal(ProposalStatus.ACCEPTED, 2000.0, 10, march);
        saveProposal(ProposalStatus.REJECTED, 300.0, 10, march);
        saveProposal(ProposalStatus.REJECTED, 300.0, 10, march);
        saveProposal(ProposalStatus.WITHDRAWN, 400.0, 10, march);
        saveProposal(ProposalStatus.WITHDRAWN, 400.0, 10, march);
        saveProposal(ProposalStatus.SUBMITTED, 600.0, 12, march);
        saveProposal(ProposalStatus.SUBMITTED, 600.0, 12, march);

        mockMvc.perform(get("/api/proposals/analytics/dashboard")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProposals").value(10))
                .andExpect(jsonPath("$.acceptanceRate").value(0.4))
                .andExpect(jsonPath("$.averageBidAmount").value(760.0))
                .andExpect(jsonPath("$.averageEstimatedDays").value(10.4))
                .andExpect(jsonPath("$.proposalsByStatus.ACCEPTED").value(4))
                .andExpect(jsonPath("$.proposalsByStatus.REJECTED").value(2))
                .andExpect(jsonPath("$.proposalsByStatus.WITHDRAWN").value(2))
                .andExpect(jsonPath("$.proposalsByStatus.SUBMITTED").value(2));
    }

    @Test
    void dashboard_emptyRange_returnsZeros() throws Exception {
        mockMvc.perform(get("/api/proposals/analytics/dashboard")
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProposals").value(0))
                .andExpect(jsonPath("$.acceptanceRate").value(0.0))
                .andExpect(jsonPath("$.averageBidAmount").value(0.0))
                .andExpect(jsonPath("$.averageEstimatedDays").value(0.0))
                .andExpect(jsonPath("$.proposalsByStatus").isEmpty());
    }

    @Test
    void dashboard_rejectsStartAfterEnd() throws Exception {
        mockMvc.perform(get("/api/proposals/analytics/dashboard")
                        .param("startDate", "2026-04-01")
                        .param("endDate", "2026-03-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithAnonymousUser
    void dashboard_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/proposals/analytics/dashboard")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31"))
                .andExpect(status().isUnauthorized());
    }

    private void saveProposal(ProposalStatus status, double bidAmount, int estimatedDays, LocalDateTime submittedAt) {
        Proposal proposal = new Proposal();
        proposal.setJobId(job.getId());
        proposal.setFreelancerId(freelancer.getId());
        proposal.setCoverLetter("Dashboard letter");
        proposal.setBidAmount(bidAmount);
        proposal.setEstimatedDays(estimatedDays);
        proposal.setStatus(status);
        proposal.setSubmittedAt(submittedAt);
        proposalRepository.save(proposal);
    }
}
