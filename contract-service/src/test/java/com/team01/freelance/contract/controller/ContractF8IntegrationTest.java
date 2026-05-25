package com.team01.freelance.contract.controller;

import com.team01.freelance.contract.model.ContractStatus;
import com.team01.freelance.contract.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.test.context.support.WithMockUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static com.team01.freelance.contract.support.ContractJdbcFixtures.insertJob;
import static com.team01.freelance.contract.support.ContractJdbcFixtures.insertUser;
import static com.team01.freelance.contract.support.ContractJdbcFixtures.saveContract;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [S4-F8] Integration tests for {@code GET /api/contracts/freelancer/{freelancerId}/summary}.
 */
@Transactional
@WithMockUser(roles = "ADMIN")
class ContractF8IntegrationTest extends AbstractIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(webApplicationContext);
    }

    @Test
    void freelancerSummary_returnsAggregatedPerformance() throws Exception {
        insertUser(jdbcTemplate, 941L, "Mariam");
        insertUser(jdbcTemplate, 942L, "Client");
        insertJob(jdbcTemplate, 841L, 942L, "Summary Job");
        saveContract(jdbcTemplate, 7401L, 841L, 941L, 942L, 1000.0, ContractStatus.COMPLETED,
                LocalDateTime.of(2026, 3, 5, 9, 0),
                LocalDateTime.of(2026, 3, 1, 9, 0),
                LocalDateTime.of(2026, 3, 10, 9, 0), null);
        saveContract(jdbcTemplate, 7402L, 841L, 941L, 942L, 2000.0, ContractStatus.COMPLETED,
                LocalDateTime.of(2026, 3, 20, 9, 0),
                LocalDateTime.of(2026, 3, 15, 9, 0),
                LocalDateTime.of(2026, 3, 25, 9, 0), null);

        mockMvc.perform(get("/api/contracts/freelancer/{freelancerId}/summary", 941L)
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.freelancerId").value(941))
                .andExpect(jsonPath("$.totalContracts").value(2))
                .andExpect(jsonPath("$.totalEarnings").value(3000.0));
    }
}