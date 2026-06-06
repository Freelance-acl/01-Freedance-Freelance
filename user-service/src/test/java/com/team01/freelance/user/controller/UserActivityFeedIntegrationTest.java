package com.team01.freelance.user.controller;

import com.jayway.jsonpath.JsonPath;
import com.team01.freelance.user.event.AuthEvent;
import com.team01.freelance.user.model.UserRole;
import com.team01.freelance.user.repository.AuthEventRepository;
import com.team01.freelance.user.repository.UserRepository;
import com.team01.freelance.user.service.UserService;
import com.team01.freelance.user.support.AbstractIntegrationTest;
import com.team01.freelance.user.support.UserTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UserActivityFeedIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserService userService;

    private MockMvc mockMvc;
    private AuthEventRepository authEventRepositoryMock;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(webApplicationContext);
        UserTestFixtures.seedAdmin(userRepository);

        authEventRepositoryMock = mock(AuthEventRepository.class);
        ReflectionTestUtils.setField(userService, "authEventRepository", authEventRepositoryMock);

        StringRedisTemplate redisTemplateMock = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperationsMock = mock(ValueOperations.class);

        when(redisTemplateMock.opsForValue()).thenReturn(valueOperationsMock);
        when(valueOperationsMock.get(anyString())).thenReturn(null);
        doNothing().when(valueOperationsMock).set(anyString(), anyString(), any());

        ReflectionTestUtils.setField(userService, "redisTemplate", redisTemplateMock);
    }

    @Test
    void activityFeed_containsRegisterLoginAndRoleChangedEventsInNewestFirstOrder() throws Exception {
        String email = uniqueEmail("activity-feed");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email)))
                .andExpect(status().isCreated());

        Long userId = userIdForEmail(email);

        String userToken = loginToken(email);

        AuthEvent registeredEvent = new AuthEvent(
                userId,
                "REGISTERED",
                LocalDateTime.now().minusMinutes(2),
                Map.of("email", email)
        );

        AuthEvent loggedInEvent = new AuthEvent(
                userId,
                "LOGGED_IN",
                LocalDateTime.now().minusMinutes(1),
                Map.of("email", email)
        );

        AuthEvent roleChangedEvent = new AuthEvent(
                userId,
                "ROLE_CHANGED",
                LocalDateTime.now(),
                Map.of("oldRole", "CLIENT", "newRole", "FREELANCER")
        );

        when(authEventRepositoryMock.findByUserIdOrderByTimestampDesc(eq(userId), any(Pageable.class)))
                .thenReturn(
                        new PageImpl<>(List.of(loggedInEvent, registeredEvent)),
                        new PageImpl<>(List.of(roleChangedEvent, loggedInEvent, registeredEvent))
                );

        mockMvc.perform(get("/api/users/{id}/activity", userId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").value("LOGGED_IN"))
                .andExpect(jsonPath("$.content[1].action").value("REGISTERED"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(2));

        String adminToken = loginToken(UserTestFixtures.SEED_ADMIN_EMAIL);

        mockMvc.perform(put("/api/users/{id}/role", userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"FREELANCER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value(UserRole.FREELANCER.name()));

        mockMvc.perform(get("/api/users/{id}/activity", userId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").value("ROLE_CHANGED"))
                .andExpect(jsonPath("$.content[1].action").value("LOGGED_IN"))
                .andExpect(jsonPath("$.content[2].action").value("REGISTERED"))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    private String loginToken(String email) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, UserTestFixtures.SEED_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return JsonPath.read(response, "$.token");
    }

    private Long userIdForEmail(String email) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?",
                Long.class,
                email
        );
    }

    private static String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@test.dev";
    }

    private static String uniquePhone() {
        return "+1555" + Math.abs(UUID.randomUUID().hashCode() % 10_000_000);
    }

    private static String registerBody(String email) {
        return """
                {
                  "name": "Activity Feed User",
                  "email": "%s",
                  "password": "%s",
                  "phone": "%s"
                }
                """.formatted(email, UserTestFixtures.SEED_PASSWORD, uniquePhone());
    }
}