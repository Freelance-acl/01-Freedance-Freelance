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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [S2-F4] Integration tests for {@code PUT /api/jobs/{id}/close}.
 */
@Transactional
class CloseJobIntegrationTest extends AbstractIntegrationTest {

    private static final String CLOSE_URL = "/api/jobs/{id}/close";
    private static final String CLOSE_BODY = "{\"status\":\"CLOSED\"}";

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
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        long suffix = System.nanoTime();
        client = saveUser("Client", "client-" + suffix + "@test.dev", UserRole.CLIENT);
        freelancer = saveUser("Freelancer", "freelancer-" + suffix + "@test.dev", UserRole.FREELANCER);
    }

    /**
     * Spec scenarios (a)–(d): active contract blocks close; after completion close succeeds and proposals rejected.
     */
    @Test
    void closeJob_withActiveContract_returns400_thenSucceedsAfterCompletion() throws Exception {
        Job job = saveOpenJob();
        Long contractId = insertContract(job.getId(), "ACTIVE");
        insertProposal(job.getId(), "SUBMITTED");
        insertProposal(job.getId(), "SUBMITTED");

        mockMvc.perform(put(CLOSE_URL, job.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CLOSE_BODY))
                .andExpect(status().isBadRequest());

        jdbcTemplate.update("UPDATE contracts SET status = ? WHERE id = ?", "COMPLETED", contractId);

        mockMvc.perform(put(CLOSE_URL, job.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CLOSE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        assertEquals(JobStatus.CLOSED, jobRepository.findById(job.getId()).orElseThrow().getStatus());
        assertEquals(0, countProposalsByStatus(job.getId(), "SUBMITTED"));
        assertEquals(2, countProposalsByStatus(job.getId(), "REJECTED"));
    }

    @Test
    void closeJob_unknownJob_returns404() throws Exception {
        mockMvc.perform(put(CLOSE_URL, 999_999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CLOSE_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void closeJob_invalidStatus_returns400() throws Exception {
        Job job = saveOpenJob();

        mockMvc.perform(put(CLOSE_URL, job.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isBadRequest());
    }

    private User saveUser(String name, String email, UserRole role) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword("secret");
        user.setPhone("+2000" + (System.nanoTime() % 1_000_000L));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    private Job saveOpenJob() {
        Job job = new Job();
        job.setClientId(client.getId());
        job.setTitle("Integration Job");
        job.setDescription("Close job test");
        job.setCategory(JobCategory.WEB_DEV);
        job.setStatus(JobStatus.OPEN);
        job.setBudgetMin(100.0);
        job.setBudgetMax(500.0);
        return jobRepository.save(job);
    }

    private Long insertContract(Long jobId, String status) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                INSERT INTO contracts (job_id, freelancer_id, client_id, proposal_id, agreed_amount, status,
                                       start_date, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                jobId, freelancer.getId(), client.getId(), 1L, 250.0, status,
                Timestamp.valueOf(now), Timestamp.valueOf(now));
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM contracts WHERE job_id = ?", Long.class, jobId);
    }

    private void insertProposal(Long jobId, String status) {
        jdbcTemplate.update("""
                INSERT INTO proposals (job_id, freelancer_id, cover_letter, bid_amount, estimated_days, status,
                                       submitted_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                jobId, freelancer.getId(), "Interested", 200.0, 7, status, Timestamp.valueOf(LocalDateTime.now()));
    }

    private int countProposalsByStatus(Long jobId, String status) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM proposals WHERE job_id = ? AND status = ?",
                Integer.class, jobId, status);
        return count != null ? count : 0;
    }
}
