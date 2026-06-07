package com.team01.freelance.contract.controller;

import com.team01.freelance.contract.security.JwtService;
import com.team01.freelance.contract.support.AbstractIntegrationTest;
import com.team01.freelance.user.model.UserRole;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class ContractSecurityAnalyticsIntegrationTest extends AbstractIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter springSecurityFilterChain;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    @Test
    void healthEndpointAllowsAnonymousAccessThroughSecurityChain() throws Exception {
        mockMvc.perform(get("/api/contracts/health"))
                .andExpect(status().isOk());
    }

    @Test
    void analyticsEndpointWithoutJwtReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/contracts/analytics")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void analyticsEndpointWithJwtReturnsAggregatedMetrics() throws Exception {
        insertUser(100L, "analytics@test.dev", "Analytics User", UserRole.CLIENT);
        insertContract(9001L, 501L, 601L, 701L, 8001L, 1000.0, "COMPLETED",
                LocalDateTime.of(2026, 3, 1, 9, 0),
                LocalDateTime.of(2026, 3, 11, 9, 0),
                LocalDateTime.of(2026, 3, 1, 8, 0));
        insertContract(9002L, 502L, 602L, 702L, 8002L, 3000.0, "ACTIVE",
                LocalDateTime.of(2026, 3, 5, 9, 0),
                null,
                LocalDateTime.of(2026, 3, 5, 8, 0));

        mockMvc.perform(get("/api/contracts/analytics")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31")
                        .header("Authorization", bearerToken(100L, "analytics@test.dev", UserRole.CLIENT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalContracts").value(2))
                .andExpect(jsonPath("$.averageContractValue").value(2000.0))
                .andExpect(jsonPath("$.completionRate").value(0.5))
                .andExpect(jsonPath("$.averageContractDurationDays").value(10.0))
                .andExpect(jsonPath("$.contractsByStatus.ACTIVE").value(1))
                .andExpect(jsonPath("$.contractsByStatus.COMPLETED").value(1));
    }

    @Test
    void analyticsEndpointWithJwtAndInvalidRangeReturnsBadRequest() throws Exception {
        insertUser(101L, "bad-range@test.dev", "Bad Range", UserRole.ADMIN);

        mockMvc.perform(get("/api/contracts/analytics")
                        .param("startDate", "2026-03-31")
                        .param("endDate", "2026-03-01")
                        .header("Authorization", bearerToken(101L, "bad-range@test.dev", UserRole.ADMIN)))
                .andExpect(status().isBadRequest());
    }

    private String bearerToken(Long userId, String email, UserRole role) {
        return "Bearer " + jwtService.generateToken(userId, email, role);
    }

    private void insertUser(Long id, String email, String name, UserRole role) {
        jdbcTemplate.update("""
                        MERGE INTO users (id, created_at, email, name, password, phone, role, status, preferences)
                        KEY(id)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, cast(? as jsonb))
                        """,
                id,
                LocalDateTime.of(2026, 3, 1, 9, 0),
                email,
                name,
                "secret",
                "+2000" + id,
                role.name(),
                "ACTIVE",
                null
        );
    }

    private void insertContract(
            Long id,
            Long jobId,
            Long freelancerId,
            Long clientId,
            Long proposalId,
            Double agreedAmount,
            String status,
            LocalDateTime startDate,
            LocalDateTime endDate,
            LocalDateTime createdAt
    ) {
        jdbcTemplate.update("""
                        MERGE INTO contracts (
                            id, job_id, freelancer_id, client_id, proposal_id, agreed_amount,
                            status, start_date, end_date, metadata, created_at
                        )
                        KEY(id)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, cast(? as jsonb), ?)
                        """,
                id,
                jobId,
                freelancerId,
                clientId,
                proposalId,
                agreedAmount,
                status,
                startDate,
                endDate,
                null,
                createdAt
        );
    }
}
