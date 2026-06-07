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
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [S2-F10] Integration tests for {@code GET /api/jobs/search/full-text}.
 *
 * In the test profile, {@code NoOpJobFullTextSearchService} is active and always returns an empty
 * page, so these tests focus on HTTP contract: required params, auth protection, and shape of the
 * success response.
 */
@Transactional
@WithMockUser(roles = "ADMIN")
class JobFullTextSearchIntegrationTest extends AbstractIntegrationTest {

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
        mockMvc = buildMockMvc(webApplicationContext);
        client = saveUser("FTS Client", "fts-" + System.nanoTime() + "@test.dev", UserRole.CLIENT);
    }

    @Test
    void fullTextSearch_withValidQuery_returns200AndEmptyPage() throws Exception {
        saveJob("Java Developer Needed", JobStatus.OPEN, 1000.0, 5000.0);

        mockMvc.perform(get("/api/jobs/search/full-text")
                        .param("query", "Java developer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void fullTextSearch_withCategoryAndStatusFilters_returns200() throws Exception {
        saveJob("Python ML Engineer", JobStatus.OPEN, 2000.0, 8000.0);

        mockMvc.perform(get("/api/jobs/search/full-text")
                        .param("query", "Python")
                        .param("category", "WEB_DEV")
                        .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void fullTextSearch_withBudgetFilters_returns200() throws Exception {
        mockMvc.perform(get("/api/jobs/search/full-text")
                        .param("query", "developer")
                        .param("minBudget", "500")
                        .param("maxBudget", "10000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void fullTextSearch_missingQueryParam_returns400() throws Exception {
        mockMvc.perform(get("/api/jobs/search/full-text"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithAnonymousUser
    void fullTextSearch_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/jobs/search/full-text")
                        .param("query", "developer"))
                .andExpect(status().isUnauthorized());
    }

    private User saveUser(String name, String email, UserRole role) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword("secret");
        user.setPhone("+4400" + (System.nanoTime() % 1_000_000_000L));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    private void saveJob(String title, JobStatus status, double budgetMin, double budgetMax) {
        Job job = new Job();
        job.setClientId(client.getId());
        job.setTitle(title);
        job.setDescription("Full-text search test job");
        job.setCategory(JobCategory.WEB_DEV);
        job.setStatus(status);
        job.setBudgetMin(budgetMin);
        job.setBudgetMax(budgetMax);
        jobRepository.save(job);
    }
}
