package com.team01.freelance.job.controller;

import com.team01.freelance.job.model.Job;
import com.team01.freelance.job.model.JobCategory;
import com.team01.freelance.job.model.JobStatus;
import com.team01.freelance.job.repository.JobRepository;
import com.team01.freelance.job.feign.dto.ProposalJobSummaryResponse;
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
import org.springframework.cache.CacheManager;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * JWT protection coverage for Ziad Wessam's M1 Job Service endpoints.
 */
@Transactional
class M1JwtProtectionIntegrationTest extends AbstractIntegrationTest {

    private static final String CLOSE_BODY = "{\"status\":\"CLOSED\"}";

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearCaches() {
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    }
    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;
    private String adminToken;
    private User client;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(webApplicationContext);
        adminToken = TestAuthHelper.adminToken(jwtService, userRepository);
        client = saveUser("M1 JWT Client", UserRole.CLIENT);
    }

    @Test
    void m1Endpoints_withoutToken_return401() throws Exception {
        mockMvc.perform(put("/api/jobs/{id}/close", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CLOSE_BODY))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/jobs/requirements/search")
                        .param("key", "experienceLevel")
                        .param("value", "SENIOR"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/jobs/reports/top-budget").param("limit", "2"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void m1Endpoints_withInvalidBearer_return401() throws Exception {
        mockMvc.perform(put("/api/jobs/{id}/close", 1L)
                        .header("Authorization", "Bearer bad-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CLOSE_BODY))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/jobs/requirements/search")
                        .header("Authorization", "Bearer bad-token")
                        .param("key", "experienceLevel")
                        .param("value", "SENIOR"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/jobs/reports/top-budget")
                        .header("Authorization", "Bearer bad-token")
                        .param("limit", "2"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void closeJob_withValidToken_returns200() throws Exception {
        Job job = saveJob("JWT close job", JobStatus.OPEN, 1200.0, null);

        mockMvc.perform(put("/api/jobs/{id}/close", job.getId())
                        .header("Authorization", TestAuthHelper.bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CLOSE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void requirementsSearch_withValidToken_returns200() throws Exception {
        jobRepository.deleteAll();
        saveJob("JWT senior job", JobStatus.OPEN, 2000.0, Map.of("experienceLevel", "SENIOR"));

        mockMvc.perform(get("/api/jobs/requirements/search")
                        .header("Authorization", TestAuthHelper.bearer(adminToken))
                        .param("key", "experienceLevel")
                        .param("value", "SENIOR")
                        .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("JWT senior job"));
    }

    @Test
    void topBudgetReport_withValidToken_returns200() throws Exception {
        Job job = saveJob("JWT top budget job", JobStatus.OPEN, 4000.0, Map.of("experienceLevel", "MID"));
        ProposalJobSummaryResponse summary = new ProposalJobSummaryResponse();
        summary.setTotalProposals(0L);
        when(proposalServiceClient.getJobProposalSummary(any(Long.class), isNull(), isNull())).thenReturn(summary);

        mockMvc.perform(get("/api/jobs/reports/top-budget")
                        .header("Authorization", TestAuthHelper.bearer(adminToken))
                        .param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("JWT top budget job"));
    }

    private User saveUser(String name, UserRole role) {
        User user = new User();
        user.setName(name);
        user.setEmail("m1-jwt-" + System.nanoTime() + "@test.dev");
        user.setPassword(TestAuthHelper.SEED_PASSWORD_HASH);
        user.setPhone("+2500" + (System.nanoTime() % 1_000_000_000L));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    private Job saveJob(String title, JobStatus status, double budgetMax, Map<String, Object> requirements) {
        Job job = new Job();
        job.setClientId(client.getId());
        job.setTitle(title);
        job.setDescription("M1 JWT protection test");
        job.setCategory(JobCategory.WEB_DEV);
        job.setStatus(status);
        job.setBudgetMin(100.0);
        job.setBudgetMax(budgetMax);
        if (requirements != null) {
            job.setRequirements(new LinkedHashMap<>(requirements));
        }
        return jobRepository.save(job);
    }
}
