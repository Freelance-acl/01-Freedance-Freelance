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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [S2-F6] Integration tests for {@code GET /api/jobs/reports/top-budget}.
 */
@Transactional
@WithMockUser(roles = "ADMIN")
class JobTopBudgetIntegrationTest extends AbstractIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User client;
    private User freelancer;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(webApplicationContext);
        client = saveUser("Client", UserRole.CLIENT);
        freelancer = saveUser("Freelancer", UserRole.FREELANCER);
    }

    @Test
    void topBudgetJobs_returnsJobsOrderedByBudgetWithProposalCounts() throws Exception {
        Job high = saveJob("High budget", 8000.0);
        Job mid = saveJob("Mid budget", 3000.0);
        insertProposal(high.getId());
        insertProposal(high.getId());
        insertProposal(mid.getId());

        mockMvc.perform(get("/api/jobs/reports/top-budget").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("High budget"))
                .andExpect(jsonPath("$[0].budgetMax").value(8000.0))
                .andExpect(jsonPath("$[0].totalProposals").value(2))
                .andExpect(jsonPath("$[1].title").value("Mid budget"));
    }

    private void insertProposal(Long jobId) {
        jdbcTemplate.update("""
                INSERT INTO proposals (job_id, freelancer_id, cover_letter, bid_amount, estimated_days, status,
                                       submitted_at)
                VALUES (?, ?, ?, ?, ?, 'SUBMITTED', CURRENT_TIMESTAMP)
                """,
                jobId, freelancer.getId(), "Bid", 100.0, 5);
    }

    private User saveUser(String name, UserRole role) {
        User user = new User();
        user.setName(name);
        user.setEmail("top-budget-" + System.nanoTime() + "@test.dev");
        user.setPassword("secret");
        user.setPhone("+2400" + (System.nanoTime() % 1_000_000_000L));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    private Job saveJob(String title, double budgetMax) {
        Job job = new Job();
        job.setClientId(client.getId());
        job.setTitle(title);
        job.setDescription("Top budget test");
        job.setCategory(JobCategory.WEB_DEV);
        job.setStatus(JobStatus.OPEN);
        job.setBudgetMin(100.0);
        job.setBudgetMax(budgetMax);
        return jobRepository.save(job);
    }
}