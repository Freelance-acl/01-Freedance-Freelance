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
 * [S1-F6] Integration tests for {@code GET /api/users/reports/top-freelancers}.
 */
@Transactional
class UserTopFreelancersIntegrationTest extends AbstractIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User freelancerA;
    private User freelancerB;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        freelancerA = saveFreelancer("User A");
        freelancerB = saveFreelancer("User B");
    }

    @Test
    void topFreelancers_returnsOrderedByEarnings() throws Exception {
        insertCompletedContract(freelancerA.getId(), 3000.0, LocalDateTime.of(2026, 3, 10, 12, 0));
        insertCompletedContract(freelancerB.getId(), 8000.0, LocalDateTime.of(2026, 3, 12, 12, 0));
        insertCompletedContract(freelancerA.getId(), 1000.0, LocalDateTime.of(2026, 3, 15, 12, 0));

        mockMvc.perform(get("/api/users/reports/top-freelancers")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31")
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("User B"))
                .andExpect(jsonPath("$[0].totalEarnings").value(8000.0))
                .andExpect(jsonPath("$[1].name").value("User A"))
                .andExpect(jsonPath("$[1].totalEarnings").value(4000.0));
    }

    private User saveFreelancer(String name) {
        User user = new User();
        user.setName(name);
        user.setEmail("top-" + System.nanoTime() + "@test.dev");
        user.setPassword("secret");
        user.setPhone("+7000" + (System.nanoTime() % 1_000_000_000L));
        user.setRole(UserRole.FREELANCER);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    private void insertCompletedContract(Long freelancerId, double amount, LocalDateTime endDate) {
        jdbcTemplate.update("""
                INSERT INTO contracts (job_id, freelancer_id, client_id, proposal_id, agreed_amount,
                                       status, start_date, end_date, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                1L, freelancerId, 2L, 3L, amount, "COMPLETED",
                Timestamp.valueOf(endDate.minusDays(5)),
                Timestamp.valueOf(endDate),
                Timestamp.valueOf(endDate));
    }
}
