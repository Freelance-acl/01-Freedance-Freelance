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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [S2-F1] Integration tests for {@code GET /api/jobs/search}.
 */
@Transactional
class JobSearchIntegrationTest extends AbstractIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

    private User client;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        client = saveUser("Client", "client-" + System.nanoTime() + "@test.dev", UserRole.CLIENT);
    }

    @Test
    void searchByStatusAndBudget_returnsMatchingOpenJobs() throws Exception {
        saveJob("Open mid budget", JobStatus.OPEN, 1000.0, 3000.0);
        saveJob("Open high budget", JobStatus.OPEN, 4000.0, 6000.0);
        saveJob("Closed mid budget", JobStatus.CLOSED, 1000.0, 3000.0);

        mockMvc.perform(get("/api/jobs/search")
                        .param("status", "OPEN")
                        .param("minBudget", "500")
                        .param("maxBudget", "3500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Open mid budget"));
    }

    @Test
    void searchWithInvalidBudgetRange_returns400() throws Exception {
        mockMvc.perform(get("/api/jobs/search")
                        .param("minBudget", "5000")
                        .param("maxBudget", "1000"))
                .andExpect(status().isBadRequest());
    }

    private User saveUser(String name, String email, UserRole role) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword("secret");
        user.setPhone("+2000" + (System.nanoTime() % 1_000_000_000L));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    private void saveJob(String title, JobStatus status, double budgetMin, double budgetMax) {
        Job job = new Job();
        job.setClientId(client.getId());
        job.setTitle(title);
        job.setDescription("Search test job");
        job.setCategory(JobCategory.WEB_DEV);
        job.setStatus(status);
        job.setBudgetMin(budgetMin);
        job.setBudgetMax(budgetMax);
        jobRepository.save(job);
    }
}
