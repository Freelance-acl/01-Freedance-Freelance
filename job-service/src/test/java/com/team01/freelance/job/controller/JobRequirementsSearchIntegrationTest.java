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

import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [S2-F5] Integration tests for {@code GET /api/jobs/requirements/search}.
 */
@Transactional
class JobRequirementsSearchIntegrationTest extends AbstractIntegrationTest {

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
        client = saveUser("Client", UserRole.CLIENT);
    }

    @Test
    void searchByRequirements_returnsMatchingOpenJobs() throws Exception {
        saveJob("Senior open", JobStatus.OPEN, Map.of("experienceLevel", "SENIOR"));
        saveJob("Senior closed", JobStatus.CLOSED, Map.of("experienceLevel", "SENIOR"));
        saveJob("Junior open", JobStatus.OPEN, Map.of("experienceLevel", "JUNIOR"));

        mockMvc.perform(get("/api/jobs/requirements/search")
                        .param("key", "experienceLevel")
                        .param("value", "SENIOR")
                        .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Senior open"));
    }

    private User saveUser(String name, UserRole role) {
        User user = new User();
        user.setName(name);
        user.setEmail("req-search-" + System.nanoTime() + "@test.dev");
        user.setPassword("secret");
        user.setPhone("+2300" + (System.nanoTime() % 1_000_000_000L));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    private void saveJob(String title, JobStatus status, Map<String, Object> requirements) {
        Job job = new Job();
        job.setClientId(client.getId());
        job.setTitle(title);
        job.setDescription("Requirements search test");
        job.setCategory(JobCategory.WEB_DEV);
        job.setStatus(status);
        job.setBudgetMin(100.0);
        job.setBudgetMax(500.0);
        job.setRequirements(new LinkedHashMap<>(requirements));
        jobRepository.save(job);
    }
}
