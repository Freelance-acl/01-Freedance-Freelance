package com.team01.freelance.user.controller;

import com.team01.freelance.user.repository.UserRepository;
import com.team01.freelance.user.service.JwtService;
import com.team01.freelance.user.support.AbstractIntegrationTest;
import com.team01.freelance.user.support.TestAuthHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * M2 authentication scenarios (a–g) for user-service.
 */
@Transactional
class M2AuthIntegrationTest extends AbstractIntegrationTest {

    private static final String SEARCH_URL = "/api/users/search";
    private static final String PASSWORD = "securePassword123";

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

    // (a) POST /api/auth/register — public, no Authorization header
    @Test
    void register_withoutAuthorizationHeader_succeeds() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(uniqueEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    // (b) POST /api/auth/login — public
    @Test
    void login_withoutToken_succeeds() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    // (c) Protected M1 endpoint without token → 401
    @Test
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(SEARCH_URL).param("role", "CLIENT"))
                .andExpect(status().isUnauthorized());
    }

    // (d) Protected endpoint with invalid Bearer → 401
    @Test
    void protectedEndpoint_withInvalidBearer_returns401() throws Exception {
        mockMvc.perform(get(SEARCH_URL)
                        .header("Authorization", "Bearer invalid-token")
                        .param("role", "CLIENT"))
                .andExpect(status().isUnauthorized());
    }

    // (e) Protected endpoint with valid token → 200
    @Test
    void protectedEndpoint_withValidToken_returns200() throws Exception {
        mockMvc.perform(get(SEARCH_URL)
                        .header("Authorization", TestAuthHelper.bearer(adminToken))
                        .param("role", "CLIENT"))
                .andExpect(status().isOk());
    }

    // (f) User CRUD — 401 without token, expected status with token
    @Test
    void userCrud_withoutToken_returns401_withToken_succeeds() throws Exception {
        mockMvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/users/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/users/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/users/1")).andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/users")
                        .header("Authorization", TestAuthHelper.bearer(adminToken)))
                .andExpect(status().isOk());

        String createBody = """
                {
                  "name": "CRUD User",
                  "email": "%s",
                  "password": "%s",
                  "phone": "+15559998877",
                  "role": "CLIENT",
                  "status": "ACTIVE"
                }
                """.formatted(uniqueEmail(), PASSWORD);

        String location = mockMvc.perform(post("/api/users")
                        .header("Authorization", TestAuthHelper.bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long id = ((Number) com.jayway.jsonpath.JsonPath.read(location, "$.id")).longValue();

        mockMvc.perform(get("/api/users/{id}", id)
                        .header("Authorization", TestAuthHelper.bearer(adminToken)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/users/{id}", id)
                        .header("Authorization", TestAuthHelper.bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated CRUD User\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/users/{id}", id)
                        .header("Authorization", TestAuthHelper.bearer(adminToken)))
                .andExpect(status().isNoContent());
    }

    // (g) Health endpoint — 200 without token
    @Test
    void health_withoutToken_returns200() throws Exception {
        mockMvc.perform(get("/api/users/health"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string("OK"));
    }

    private static String uniqueEmail() {
        return "m2auth-" + UUID.randomUUID() + "@test.dev";
    }

    private static String registerBody(String email) {
        return """
                {
                  "name": "M2 Auth User",
                  "email": "%s",
                  "password": "%s",
                  "phone": "+15551234001"
                }
                """.formatted(email, PASSWORD);
    }
}
