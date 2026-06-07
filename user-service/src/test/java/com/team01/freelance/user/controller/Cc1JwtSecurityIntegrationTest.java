package com.team01.freelance.user.controller;

import com.team01.freelance.user.config.JwtConfig;
import com.team01.freelance.user.model.ProficiencyLevel;
import com.team01.freelance.user.model.User;
import com.team01.freelance.user.model.UserRole;
import com.team01.freelance.user.model.UserSkill;
import com.team01.freelance.user.repository.UserRepository;
import com.team01.freelance.user.repository.UserSkillRepository;
import com.team01.freelance.user.service.JwtService;
import com.team01.freelance.user.support.AbstractIntegrationTest;
import com.team01.freelance.user.support.JwtTestSupport;
import com.team01.freelance.user.support.TestAuthHelper;
import com.team01.freelance.user.support.UserTestFixtures;
import com.team01.freelance.user.support.cc1.Cc1EndpointScanner;
import com.team01.freelance.user.support.cc1.Cc1EndpointScanner.Endpoint;
import com.team01.freelance.user.support.cc1.Cc1PublicEndpoints;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CC-1 — JWT required on all endpoints except register, login, and health.
 */
@Transactional
class Cc1JwtSecurityIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSkillRepository userSkillRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtConfig jwtConfig;

    private MockMvc mockMvc;
    private List<Endpoint> endpoints;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(webApplicationContext);
        endpoints = Cc1EndpointScanner.scan(applicationContext);
        assertFalse(endpoints.isEmpty(), "expected controller endpoints from handler mapping scan");
    }

    /** (a) Every non-public endpoint returns 401 without Authorization. */
    @Test
    void allProtectedEndpoints_withoutAuth_return401() throws Exception {
        for (Endpoint endpoint : endpoints) {
            if (endpoint.isPublic()) {
                continue;
            }
            mockMvc.perform(Cc1EndpointScanner.mockRequest(endpoint))
                    .andExpect(status().isUnauthorized());
        }
    }

    /** (b) Public endpoints succeed without a token. */
    @Test
    void publicEndpoints_withoutAuth_return2xx() throws Exception {
        for (Endpoint endpoint : endpoints) {
            if (!endpoint.isPublic()) {
                continue;
            }
            if (Cc1PublicEndpoints.REGISTER.equals(endpoint.path()) && HttpMethod.POST.equals(endpoint.method())) {
                String email = "cc1-public-" + UUID.randomUUID() + "@test.dev";
                mockMvc.perform(post(endpoint.path())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "CC1 User",
                                          "email": "%s",
                                          "password": "%s",
                                          "phone": "+15551234099"
                                        }
                                        """.formatted(email, UserTestFixtures.SEED_PASSWORD)))
                        .andExpect(status().isCreated());
            } else if (Cc1PublicEndpoints.LOGIN.equals(endpoint.path()) && HttpMethod.POST.equals(endpoint.method())) {
                UserTestFixtures.seedAdmin(userRepository);
                mockMvc.perform(post(endpoint.path())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "email": "%s",
                                          "password": "%s"
                                        }
                                        """.formatted(UserTestFixtures.SEED_ADMIN_EMAIL, UserTestFixtures.SEED_PASSWORD)))
                        .andExpect(status().isOk());
            } else if (endpoint.path().endsWith("/health")) {
                mockMvc.perform(get(endpoint.path())).andExpect(status().isOk());
            }
        }
    }

    /** (c) Malformed Bearer token on a protected endpoint → 401. */
    @Test
    void protectedEndpoint_withMalformedBearer_returns401() throws Exception {
        Endpoint sample = endpoints.stream()
                .filter(e -> !e.isPublic())
                .findFirst()
                .orElseThrow();
        mockMvc.perform(Cc1EndpointScanner.mockRequest(sample).header("Authorization", "Bearer abc"))
                .andExpect(status().isUnauthorized());
    }

    /** (d) Expired token on a protected endpoint → 401. */
    @Test
    void assignedUserM1Endpoints_withoutToken_return401() throws Exception {
        List<MockHttpServletRequestBuilder> assignedEndpoints = List.of(
                get("/api/users"),
                get("/api/users/1"),
                post("/api/users").contentType(MediaType.APPLICATION_JSON).content("{}"),
                put("/api/users/1/deactivate"),
                get("/api/users/preferences/search").param("key", "language").param("value", "ar"),
                get("/api/users/reports/top-freelancers")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31")
                        .param("limit", "2"),
                put("/api/users/1").contentType(MediaType.APPLICATION_JSON).content("{}"),
                delete("/api/users/1"),
                delete("/api/users/all"),
                get("/api/users/search").param("role", "CLIENT"),
                put("/api/users/1/preferences").contentType(MediaType.APPLICATION_JSON).content("{}"),
                get("/api/users/1/contract-summary"),
                put("/api/users/1/skills/1/primary"),
                get("/api/users/1/profile"),
                get("/api/users/preferences/language").param("lang", "ar").param("minContracts", "0"),
                get("/api/user-skills"),
                get("/api/user-skills/1"),
                post("/api/user-skills").contentType(MediaType.APPLICATION_JSON).content("{}"),
                put("/api/user-skills/1").contentType(MediaType.APPLICATION_JSON).content("{}"),
                delete("/api/user-skills/1"),
                delete("/api/user-skills/all"));

        for (MockHttpServletRequestBuilder request : assignedEndpoints) {
            mockMvc.perform(request).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void assignedUserReadEndpoints_withValidToken_succeed() throws Exception {
        User admin = UserTestFixtures.seedAdmin(userRepository);
        UserSkill skill = saveSkill(admin);
        String bearerToken = TestAuthHelper.bearer(jwtService.generateToken(admin));

        mockMvc.perform(get("/api/users").header("Authorization", bearerToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/users/{id}", admin.getId()).header("Authorization", bearerToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/users/search")
                        .header("Authorization", bearerToken)
                        .param("email", admin.getEmail()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/user-skills").header("Authorization", bearerToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/user-skills/{id}", skill.getId()).header("Authorization", bearerToken))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpoint_withExpiredToken_returns401() throws Exception {
        User admin = UserTestFixtures.seedAdmin(userRepository);
        String expired = JwtTestSupport.expiredToken(admin, jwtConfig);
        Endpoint sample = endpoints.stream()
                .filter(e -> !e.isPublic())
                .findFirst()
                .orElseThrow();
        mockMvc.perform(Cc1EndpointScanner.mockRequest(sample).header("Authorization", TestAuthHelper.bearer(expired)))
                .andExpect(status().isUnauthorized());
    }

    /** (e) CLIENT token on PUT /api/users/{id}/role → 403. */
    @Test
    void updateRole_withClientToken_returns403() throws Exception {
        User target = UserTestFixtures.saveUser(
                userRepository,
                "Role Target",
                "cc1-role-target-" + UUID.randomUUID() + "@test.dev",
                "+1555" + (System.nanoTime() % 10_000_000L),
                UserRole.CLIENT,
                UserTestFixtures.SEED_PASSWORD);
        String clientToken = TestAuthHelper.clientToken(jwtService, userRepository);

        mockMvc.perform(put("/api/users/{id}/role", target.getId())
                        .header("Authorization", TestAuthHelper.bearer(clientToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"FREELANCER\"}"))
                .andExpect(status().isForbidden());
    }

    /**
     * (f) Platform exposes exactly three public endpoint categories: register, login, health.
     * Auth paths exist only on user-service; every service exposes a health check.
     */
    @Test
    void publicEndpointCategories_areExactlyRegisterLoginAndHealth() {
        Set<String> categories = new HashSet<>();
        long publicCount = 0;
        long protectedCount = 0;

        for (Endpoint endpoint : endpoints) {
            if (endpoint.isPublic()) {
                publicCount++;
                String category = Cc1PublicEndpoints.category(endpoint.method(), endpoint.path());
                assertTrue(category != null, () -> "unexpected public endpoint: " + endpoint);
                categories.add(category);
            } else {
                protectedCount++;
            }
        }

        assertEquals(Set.of("register", "login", "health"), categories);
        assertEquals(3, categories.size());
        assertTrue(publicCount >= 3, "expected at least register, login, and health");
        assertTrue(protectedCount > 0, "expected protected M1/CC endpoints");
    }

    private UserSkill saveSkill(User user) {
        UserSkill skill = new UserSkill();
        skill.setUser(user);
        skill.setSkillName("Spring Security");
        skill.setCategory("Backend");
        skill.setYearsOfExperience(2);
        skill.setProficiencyLevel(ProficiencyLevel.INTERMEDIATE);
        skill.setIsPrimary(false);
        return userSkillRepository.save(skill);
    }

}
