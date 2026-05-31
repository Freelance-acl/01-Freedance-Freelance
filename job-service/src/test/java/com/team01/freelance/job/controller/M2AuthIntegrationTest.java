package com.team01.freelance.job.controller;

import com.team01.freelance.job.model.JobStatus;
import com.team01.freelance.job.support.AbstractIntegrationTest;
import com.team01.freelance.job.support.TestAuthHelper;
import com.team01.freelance.user.model.User;
import com.team01.freelance.user.model.UserRole;
import com.team01.freelance.user.model.UserStatus;
import com.team01.freelance.user.repository.UserRepository;
import com.team01.freelance.user.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * M2 authentication for job-service (c–g, job CRUD).
 */
@Transactional
class M2AuthIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;
    private String adminToken;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(webApplicationContext);
        adminToken = TestAuthHelper.adminToken(jwtService, userRepository);
    }

    @Test
    void jobSearch_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/jobs/search").param("status", "OPEN"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void jobSearch_withInvalidBearer_returns401() throws Exception {
        mockMvc.perform(get("/api/jobs/search")
                        .header("Authorization", "Bearer bad-token")
                        .param("status", "OPEN"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void jobSearch_withValidToken_returns200() throws Exception {
        saveClient();
        mockMvc.perform(get("/api/jobs/search")
                        .header("Authorization", TestAuthHelper.bearer(adminToken))
                        .param("status", "OPEN")
                        .param("minBudget", "0")
                        .param("maxBudget", "100000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void jobCrud_withoutToken_returns401_withToken_succeeds() throws Exception {
        User client = saveClient();

        mockMvc.perform(get("/api/jobs")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/jobs/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/jobs").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/jobs/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/jobs/1")).andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/jobs")
                        .header("Authorization", TestAuthHelper.bearer(adminToken)))
                .andExpect(status().isOk());

        String body = """
                {
                  "clientId": %d,
                  "title": "M2 Auth Job",
                  "description": "desc",
                  "category": "WEB_DEV",
                  "status": "OPEN",
                  "budgetMin": 100,
                  "budgetMax": 500
                }
                """.formatted(client.getId());

        String created = mockMvc.perform(post("/api/jobs")
                        .header("Authorization", TestAuthHelper.bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long id = ((Number) com.jayway.jsonpath.JsonPath.read(created, "$.id")).longValue();

        mockMvc.perform(get("/api/jobs/{id}", id)
                        .header("Authorization", TestAuthHelper.bearer(adminToken)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/jobs/{id}", id)
                        .header("Authorization", TestAuthHelper.bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated M2 Job\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/jobs/{id}", id)
                        .header("Authorization", TestAuthHelper.bearer(adminToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void health_withoutToken_returns200() throws Exception {
        mockMvc.perform(get("/api/jobs/health"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string("OK"));
    }

    private User saveClient() {
        User client = new User();
        client.setName("Job Client");
        client.setEmail("job-client-" + System.nanoTime() + "@test.dev");
        client.setPassword(TestAuthHelper.SEED_PASSWORD_HASH);
        client.setPhone("+1555" + (System.nanoTime() % 10_000_000L));
        client.setRole(UserRole.CLIENT);
        client.setStatus(UserStatus.ACTIVE);
        return userRepository.save(client);
    }
}
