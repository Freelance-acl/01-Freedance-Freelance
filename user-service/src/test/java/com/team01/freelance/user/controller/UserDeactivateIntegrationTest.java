package com.team01.freelance.user.controller;

import com.team01.freelance.common.observer.EntityObserver;
import com.team01.freelance.common.observer.EventSubject;
import com.team01.freelance.user.model.User;
import com.team01.freelance.user.model.UserRole;
import com.team01.freelance.user.model.UserStatus;
import com.team01.freelance.user.repository.UserRepository;
import com.team01.freelance.user.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.test.context.support.WithMockUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [S1-F4] Integration tests for {@code PUT /api/users/{id}/deactivate}.
 */
@Transactional
@WithMockUser(roles = "ADMIN")
class UserDeactivateIntegrationTest extends AbstractIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EventSubject authEventSubject;

    private User freelancer;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(webApplicationContext);
        freelancer = saveUser("Deactivate User", UserRole.FREELANCER);
    }

    @Test
    void deactivateUser_withoutActiveContracts_setsDeactivated() throws Exception {
        mockMvc.perform(put("/api/users/{id}/deactivate", freelancer.getId()))
                .andExpect(status().isOk());

        assertEquals(UserStatus.DEACTIVATED,
                userRepository.findById(freelancer.getId()).orElseThrow().getStatus());
    }

    @Test
    void deactivateUser_withActiveContract_returns400() throws Exception {
        insertActiveContract(freelancer.getId());

        mockMvc.perform(put("/api/users/{id}/deactivate", freelancer.getId()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deactivateUser_withoutActiveContracts_notifiesUserDeactivatedAuthEvent() throws Exception {
        List<Map<?, ?>> events = new ArrayList<>();
        EntityObserver observer = (eventType, payload) -> {
            if ("USER_DEACTIVATED".equals(eventType) && payload instanceof Map<?, ?> eventPayload) {
                events.add(eventPayload);
            }
        };
        authEventSubject.register(observer);

        try {
            mockMvc.perform(put("/api/users/{id}/deactivate", freelancer.getId()))
                    .andExpect(status().isOk());

            assertFalse(events.isEmpty());
            Map<?, ?> payload = events.get(0);
            assertEquals(freelancer.getId(), payload.get("userId"));
            assertEquals("USER_DEACTIVATED", payload.get("action"));
        } finally {
            authEventSubject.unregister(observer);
        }
    }

    private User saveUser(String name, UserRole role) {
        User user = new User();
        user.setName(name);
        user.setEmail("deact-" + System.nanoTime() + "@test.dev");
        user.setPassword("secret");
        user.setPhone("+5000" + (System.nanoTime() % 1_000_000_000L));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    private void insertActiveContract(Long freelancerId) {
        jdbcTemplate.update("""
                INSERT INTO contracts (job_id, freelancer_id, client_id, proposal_id, agreed_amount,
                                       status, start_date, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                1L, freelancerId, 2L, 3L, 1000.0, "ACTIVE",
                Timestamp.valueOf(LocalDateTime.now()),
                Timestamp.valueOf(LocalDateTime.now()));
    }
}
