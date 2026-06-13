package com.team01.freelance.job.controller;

import com.team01.freelance.job.feign.ContractServiceClient;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [S2-F4] Integration tests for {@code PUT /api/jobs/{id}/close}.
 */
@WithMockUser(roles = "ADMIN")
@Sql(statements = {
        "DELETE FROM job_attachments",
        "DELETE FROM proposals",
        "DELETE FROM contracts",
        "DELETE FROM jobs",
        "DELETE FROM user_skills",
        "DELETE FROM users"
}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
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

    private User client;
    private User freelancer;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(webApplicationContext);

        long suffix = System.nanoTime();
        client = saveUser("Client", "client-" + suffix + "@test.dev", UserRole.CLIENT);
        freelancer = saveUser("Freelancer", "freelancer-" + suffix + "@test.dev", UserRole.FREELANCER);
    }

    @Test
    void closeJob_withActiveContract_returns400_thenSucceedsAfterCompletion() throws Exception {
        Job job = saveOpenJob();
        when(contractServiceClient.getActiveContractCountForJob(eq(job.getId()))).thenReturn(1, 0);

        mockMvc.perform(put(CLOSE_URL, job.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CLOSE_BODY))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put(CLOSE_URL, job.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CLOSE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        assertEquals(JobStatus.CLOSED, jobRepository.findById(job.getId()).orElseThrow().getStatus());
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
}
