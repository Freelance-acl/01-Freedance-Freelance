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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [S2-F2] Integration tests for {@code PUT /api/jobs/{id}/requirements}.
 */
@Transactional
class JobRequirementsIntegrationTest extends AbstractIntegrationTest {

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
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        User client = saveUser("Client", UserRole.CLIENT);
        job = saveJob(client.getId(), Map.of("experienceLevel", "MID"));
    }

    @Test
    void updateRequirements_mergesIncomingFields() throws Exception {
        mockMvc.perform(put("/api/jobs/{id}/requirements", job.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"experienceLevel\":\"SENIOR\",\"duration\":\"8 weeks\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requirements.experienceLevel").value("SENIOR"))
                .andExpect(jsonPath("$.requirements.duration").value("8 weeks"));
    }

    private User saveUser(String name, UserRole role) {
        User user = new User();
        user.setName(name);
        user.setEmail("req-" + System.nanoTime() + "@test.dev");
        user.setPassword("secret");
        user.setPhone("+2100" + (System.nanoTime() % 1_000_000_000L));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    private Job saveJob(Long clientId, Map<String, Object> requirements) {
        Job job = new Job();
        job.setClientId(clientId);
        job.setTitle("Requirements job");
        job.setDescription("Merge requirements test");
        job.setCategory(JobCategory.WEB_DEV);
        job.setStatus(JobStatus.OPEN);
        job.setBudgetMin(100.0);
        job.setBudgetMax(500.0);
        job.setRequirements(new LinkedHashMap<>(requirements));
        return jobRepository.save(job);
    }
}
