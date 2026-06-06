package com.team01.freelance.user.controller;

import com.team01.freelance.common.observer.EntityObserver;
import com.team01.freelance.common.observer.EventSubject;
import com.team01.freelance.user.model.User;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    @Autowired
    private EventSubject authEventSubject;

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
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void register_withInvalidBearerToken_stillSucceeds() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(uniqueEmail())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already exists"));
    }

    // (b) POST /api/auth/login — public
    @Test
    void login_withoutToken_succeeds() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    // S1-F11 login: both invalid credentials cases must return 401.
    @Test
    void login_withWrongPassword_returns401() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"wrong-password"}
                                """.formatted(email)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void login_withUnknownEmail_returns401Not404() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(uniqueEmail(), PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void login_returnsJwtWithUidAndRoleClaimsAndTokenAccessesProtectedEndpoint() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email)))
                .andExpect(status().isCreated());

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").isNumber())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = com.jayway.jsonpath.JsonPath.read(loginResponse, "$.token");
        User user = userRepository.findByEmail(email).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(jwtService.extractUsername(token)).isEqualTo(email);
        org.assertj.core.api.Assertions.assertThat(jwtService.extractUserId(token)).isEqualTo(user.getId());
        org.assertj.core.api.Assertions.assertThat(jwtService.extractRole(token)).isEqualTo(user.getRole().name());

        mockMvc.perform(get("/api/users/{id}", user.getId())
                        .header("Authorization", TestAuthHelper.bearer(token)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/{id}", user.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_notifiesLoggedInAuthEvent() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email)))
                .andExpect(status().isCreated());

        List<Map<String, Object>> observedEvents = new ArrayList<>();
        EntityObserver observer = (eventType, payload) -> {
            if (payload instanceof Map<?, ?> payloadMap) {
                observedEvents.add(Map.of("eventType", eventType, "payload", payloadMap));
            }
        };
        authEventSubject.register(observer);
        try {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"%s","password":"%s"}
                                    """.formatted(email, PASSWORD)))
                    .andExpect(status().isOk());
        } finally {
            authEventSubject.unregister(observer);
        }

        User user = userRepository.findByEmail(email).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(observedEvents).anySatisfy(event -> {
            org.assertj.core.api.Assertions.assertThat(event.get("eventType")).isEqualTo("LOGGED_IN");
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) event.get("payload");
            org.assertj.core.api.Assertions.assertThat(payload.get("userId")).isEqualTo(user.getId());
            org.assertj.core.api.Assertions.assertThat(payload.get("action")).isEqualTo("LOGGED_IN");
            org.assertj.core.api.Assertions.assertThat(payload.get("details")).isEqualTo(Map.of());
        });
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
