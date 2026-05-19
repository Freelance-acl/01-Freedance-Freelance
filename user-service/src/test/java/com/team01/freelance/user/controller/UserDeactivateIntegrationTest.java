package com.team01.freelance.user.controller;

import com.team01.freelance.user.model.User;
import com.team01.freelance.user.model.UserRole;
import com.team01.freelance.user.model.UserStatus;
import com.team01.freelance.user.repository.UserRepository;
import com.team01.freelance.user.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [S1-F4] Integration tests for {@code PUT /api/users/{id}/deactivate}.
 */
@Transactional
class UserDeactivateIntegrationTest extends AbstractIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User freelancer;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
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
