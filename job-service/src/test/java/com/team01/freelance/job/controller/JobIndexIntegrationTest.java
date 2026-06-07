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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [S2-F11] Integration tests for {@code POST /api/jobs/{id}/index}.
 *
 * Elasticsearch is not available in the test profile, so indexing a real job produces a 500 with an
 * error body — this verifies the controller's error-handling contract.  The 404 path (unknown job)
 * is fully exercised because the entity lookup precedes any Elasticsearch call.
 */
@Transactional
@WithMockUser(roles = "ADMIN")
class JobIndexIntegrationTest extends AbstractIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

    private Job job;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(webApplicationContext);
        User client = saveUser("Index Client", "idx-" + System.nanoTime() + "@test.dev", UserRole.CLIENT);
        job = saveJob(client.getId());
    }

    @Test
    void indexJob_nonExistentJob_returns404() throws Exception {
        mockMvc.perform(post("/api/jobs/{id}/index", 999999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void indexJob_elasticsearchUnavailable_returns500WithErrorBody() throws Exception {
        mockMvc.perform(post("/api/jobs/{id}/index", job.getId()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Elasticsearch indexing failed"));
    }

    @Test
    @WithAnonymousUser
    void indexJob_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/jobs/{id}/index", job.getId()))
                .andExpect(status().isUnauthorized());
    }

    private User saveUser(String name, String email, UserRole role) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword("secret");
        user.setPhone("+5500" + (System.nanoTime() % 1_000_000_000L));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    private Job saveJob(Long clientId) {
        Job j = new Job();
        j.setClientId(clientId);
        j.setTitle("Index Test Job");
        j.setDescription("Job for index endpoint testing");
        j.setCategory(JobCategory.WEB_DEV);
        j.setStatus(JobStatus.OPEN);
        j.setBudgetMin(500.0);
        j.setBudgetMax(3000.0);
        return jobRepository.save(j);
    }
}
