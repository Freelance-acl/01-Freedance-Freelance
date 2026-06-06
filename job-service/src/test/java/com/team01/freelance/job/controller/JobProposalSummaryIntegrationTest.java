package com.team01.freelance.job.controller;

import com.team01.freelance.job.model.Job;
import com.team01.freelance.job.model.JobCategory;
import com.team01.freelance.job.model.JobStatus;
import com.team01.freelance.job.repository.JobRepository;
import com.team01.freelance.job.support.AbstractIntegrationTest;
import com.team01.freelance.user.model.User;
import com.team01.freelance.user.model.UserRole;
import com.team01.freelance.user.model.UserStatus;
import com.team01.freelance.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.test.context.support.WithMockUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [S2-F3] Integration tests for {@code GET /api/jobs/{id}/proposal-summary}.
 */
@Transactional
@WithMockUser(roles = "ADMIN")
class JobProposalSummaryIntegrationTest extends AbstractIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Job job;
    private User freelancer;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(webApplicationContext);
        User client = saveUser("Client", UserRole.CLIENT);
        freelancer = saveUser("Freelancer", UserRole.FREELANCER);
        job = saveJob(client.getId());
    }

    @Test
    void proposalSummary_aggregatesProposalsInDateRange() throws Exception {
        insertProposal(200.0, LocalDateTime.of(2026, 3, 5, 10, 0));
        insertProposal(400.0, LocalDateTime.of(2026, 3, 15, 10, 0));
        insertProposal(900.0, LocalDateTime.of(2026, 4, 1, 10, 0));

        mockMvc.perform(get("/api/jobs/{id}/proposal-summary", job.getId())
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(job.getId().intValue()))
                .andExpect(jsonPath("$.totalProposals").value(2))
                .andExpect(jsonPath("$.averageBidAmount").value(300.0))
                .andExpect(jsonPath("$.lowestBid").value(200.0))
                .andExpect(jsonPath("$.highestBid").value(400.0));
    }

    private void insertProposal(double bidAmount, LocalDateTime submittedAt) {
        jdbcTemplate.update("""
                INSERT INTO proposals (job_id, freelancer_id, cover_letter, bid_amount, estimated_days, status,
                                       submitted_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                job.getId(), freelancer.getId(), "Interested", bidAmount, 7, "SUBMITTED",
                Timestamp.valueOf(submittedAt));
    }

    private User saveUser(String name, UserRole role) {
        User user = new User();
        user.setName(name);
        user.setEmail("summary-" + System.nanoTime() + "@test.dev");
        user.setPassword("secret");
        user.setPhone("+2200" + (System.nanoTime() % 1_000_000_000L));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    private Job saveJob(Long clientId) {
        Job job = new Job();
        job.setClientId(clientId);
        job.setTitle("Summary job");
        job.setDescription("Proposal summary test");
        job.setCategory(JobCategory.WEB_DEV);
        job.setStatus(JobStatus.OPEN);
        job.setBudgetMin(100.0);
        job.setBudgetMax(1000.0);
        return jobRepository.save(job);
    }
}