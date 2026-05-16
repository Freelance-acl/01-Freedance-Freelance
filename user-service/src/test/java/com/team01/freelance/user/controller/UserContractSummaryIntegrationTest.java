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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [S1-F3] Integration tests for {@code GET /api/users/{id}/contract-summary}.
 */
@Transactional
class UserContractSummaryIntegrationTest extends AbstractIntegrationTest {

    private static final String SUMMARY_URL = "/api/users/{id}/contract-summary";

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User freelancer;
    private User client;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        long suffix = System.nanoTime();
        client = saveUser("Client", "client-" + suffix + "@test.dev", "+20001" + (suffix % 1_000_000L),
                UserRole.CLIENT);
        freelancer = saveUser("Freelancer", "freelancer-" + suffix + "@test.dev",
                "+20002" + (suffix % 1_000_000L), UserRole.FREELANCER);
    }

    /**
     * Spec scenarios (a)+(b): 5 contracts → aggregated summary metrics.
     */
    @Test
    void getContractSummary_withMixedContracts_returnsAggregatedMetrics() throws Exception {
        insertContract(freelancer.getId(), client.getId(), "COMPLETED", 500.0);
        insertContract(freelancer.getId(), client.getId(), "COMPLETED", 1000.0);
        insertContract(freelancer.getId(), client.getId(), "COMPLETED", 1500.0);
        insertContract(freelancer.getId(), client.getId(), "TERMINATED", 800.0);
        insertContract(freelancer.getId(), client.getId(), "ACTIVE", 2000.0);

        mockMvc.perform(get(SUMMARY_URL, freelancer.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(freelancer.getId().intValue()))
                .andExpect(jsonPath("$.name").value("Freelancer"))
                .andExpect(jsonPath("$.totalContracts").value(5))
                .andExpect(jsonPath("$.completedContracts").value(3))
                .andExpect(jsonPath("$.terminatedContracts").value(1))
                .andExpect(jsonPath("$.totalEarnings").value(3000.0))
                .andExpect(jsonPath("$.averageContractValue").value(1000.0));
    }

    /** Spec scenario (c): user with no contracts → all zeros. */
    @Test
    void getContractSummary_noContracts_returnsZeros() throws Exception {
        mockMvc.perform(get(SUMMARY_URL, freelancer.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(freelancer.getId().intValue()))
                .andExpect(jsonPath("$.totalContracts").value(0))
                .andExpect(jsonPath("$.completedContracts").value(0))
                .andExpect(jsonPath("$.terminatedContracts").value(0))
                .andExpect(jsonPath("$.totalEarnings").value(0.0))
                .andExpect(jsonPath("$.averageContractValue").value(0.0));
    }

    /** Spec scenario (d): unknown user → 404. */
    @Test
    void getContractSummary_unknownUser_returns404() throws Exception {
        mockMvc.perform(get(SUMMARY_URL, 999_999L))
                .andExpect(status().isNotFound());
    }

    private User saveUser(String name, String email, String phone, UserRole role) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword("secret");
        user.setPhone(phone);
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    private void insertContract(Long freelancerId, Long clientId, String status, double amount) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                INSERT INTO contracts (job_id, freelancer_id, client_id, proposal_id, agreed_amount, status,
                                       start_date, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                1L, freelancerId, clientId, 1L, amount, status,
                Timestamp.valueOf(now), Timestamp.valueOf(now));
    }
}
