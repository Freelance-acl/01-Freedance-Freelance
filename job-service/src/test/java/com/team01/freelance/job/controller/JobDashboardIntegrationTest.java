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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.sql.Date;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [S2-F12] Integration tests for {@code GET /api/jobs/{id}/dashboard}.
 */
@Transactional
@WithMockUser(roles = "ADMIN")
class JobDashboardIntegrationTest extends AbstractIntegrationTest {

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
        User client = saveUser("Dashboard Client", UserRole.CLIENT);
        freelancer = saveUser("Dashboard Freelancer", UserRole.FREELANCER);
        job = saveJob(client.getId());
    }

    @Test
    void dashboard_returnsAggregatedMetrics() throws Exception {
        insertProposal(300.0, "SUBMITTED");
        insertProposal(500.0, "ACCEPTED");
        insertActiveAttachment();

        mockMvc.perform(get("/api/jobs/{id}/dashboard", job.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(job.getId().intValue()))
                .andExpect(jsonPath("$.title").value("Dashboard Job"))
                .andExpect(jsonPath("$.totalProposals").value(2))
                .andExpect(jsonPath("$.acceptedProposals").value(1))
                .andExpect(jsonPath("$.averageBidAmount").value(400.0))
                .andExpect(jsonPath("$.activeAttachments").value(1));
    }

    @Test
    void dashboard_noProposals_returnsZeroAggregates() throws Exception {
        mockMvc.perform(get("/api/jobs/{id}/dashboard", job.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(job.getId().intValue()))
                .andExpect(jsonPath("$.totalProposals").value(0))
                .andExpect(jsonPath("$.acceptedProposals").value(0))
                .andExpect(jsonPath("$.averageBidAmount").value(0.0))
                .andExpect(jsonPath("$.activeAttachments").value(0));
    }

    @Test
    void dashboard_nonExistentJob_returns404() throws Exception {
        mockMvc.perform(get("/api/jobs/{id}/dashboard", 999999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithAnonymousUser
    void dashboard_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/jobs/{id}/dashboard", job.getId()))
                .andExpect(status().isUnauthorized());
    }

    private void insertProposal(double bidAmount, String status) {
        jdbcTemplate.update("""
                INSERT INTO proposals (job_id, freelancer_id, cover_letter, bid_amount, estimated_days, status,
                                       submitted_at)
                VALUES (?, ?, ?, ?, ?, ?, NOW())
                """,
                job.getId(), freelancer.getId(), "Cover letter", bidAmount, 7, status);
    }

    private void insertActiveAttachment() {
        jdbcTemplate.update("""
                INSERT INTO job_attachments (job_id, type, file_url, expiry_date, verified, uploaded_at)
                VALUES (?, 'BRIEF', ?, ?, false, NOW())
                """,
                job.getId(), "http://example.com/spec.pdf",
                Date.valueOf(LocalDate.now().plusDays(30)));
    }

    private User saveUser(String name, UserRole role) {
        User user = new User();
        user.setName(name);
        user.setEmail("dash-" + System.nanoTime() + "@test.dev");
        user.setPassword("secret");
        user.setPhone("+3300" + (System.nanoTime() % 1_000_000_000L));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    private Job saveJob(Long clientId) {
        Job j = new Job();
        j.setClientId(clientId);
        j.setTitle("Dashboard Job");
        j.setDescription("Dashboard test job");
        j.setCategory(JobCategory.WEB_DEV);
        j.setStatus(JobStatus.OPEN);
        j.setBudgetMin(100.0);
        j.setBudgetMax(1000.0);
        return jobRepository.save(j);
    }
}
