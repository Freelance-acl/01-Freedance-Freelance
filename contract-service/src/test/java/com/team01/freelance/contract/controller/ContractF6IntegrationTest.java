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
 * [S4-F6] Integration tests for {@code GET /api/contracts/history}.
 */
@Transactional
@WithMockUser(roles = "ADMIN")
class ContractF6IntegrationTest extends AbstractIntegrationTest {

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
    void historySearch_returnsContractsInDateRange() throws Exception {
        insertUser(jdbcTemplate, 921L, "Freelancer");
        insertUser(jdbcTemplate, 922L, "Client");
        insertJob(jdbcTemplate, 821L, 922L, "History Job");
        saveContract(jdbcTemplate, 7201L, 821L, 921L, 922L, 1000.0, ContractStatus.COMPLETED,
                LocalDateTime.of(2026, 3, 10, 9, 0),
                LocalDateTime.of(2026, 3, 1, 9, 0),
                LocalDateTime.of(2026, 3, 15, 9, 0), null);
        saveContract(jdbcTemplate, 7202L, 821L, 921L, 922L, 2000.0, ContractStatus.ACTIVE,
                LocalDateTime.of(2026, 4, 1, 9, 0),
                LocalDateTime.of(2026, 4, 1, 9, 0), null, null);

        mockMvc.perform(get("/api/contracts/history")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31")
                        .param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(7201));
    }
}