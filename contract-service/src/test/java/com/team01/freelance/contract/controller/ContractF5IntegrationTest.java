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
import java.util.Map;

import static com.team01.freelance.contract.support.ContractJdbcFixtures.insertJob;
import static com.team01.freelance.contract.support.ContractJdbcFixtures.insertUser;
import static com.team01.freelance.contract.support.ContractJdbcFixtures.saveContract;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [S4-F5] Integration tests for {@code GET /api/contracts/metadata/search}.
 */
@Transactional
@WithMockUser(roles = "ADMIN")
class ContractF5IntegrationTest extends AbstractIntegrationTest {

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
    void metadataSearch_eqOperator_returnsMatchingContracts() throws Exception {
        insertUser(jdbcTemplate, 911L, "Freelancer");
        insertUser(jdbcTemplate, 912L, "Client");
        insertJob(jdbcTemplate, 811L, 912L, "Metadata Job");
        saveContract(jdbcTemplate, 7101L, 811L, 911L, 912L, 1000.0, ContractStatus.ACTIVE,
                LocalDateTime.now(), LocalDateTime.now(), null,
                Map.of("priority", "high"));
        saveContract(jdbcTemplate, 7102L, 811L, 911L, 912L, 1200.0, ContractStatus.ACTIVE,
                LocalDateTime.now(), LocalDateTime.now(), null,
                Map.of("priority", "low"));

        mockMvc.perform(get("/api/contracts/metadata/search")
                        .param("key", "priority")
                        .param("operator", "eq")
                        .param("value", "high"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(7101));
    }
}