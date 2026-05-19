package com.team01.freelance.contract.controller;

import com.team01.freelance.contract.model.ContractStatus;
import com.team01.freelance.contract.service.ContractService;
import com.team01.freelance.contract.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.Map;

import static com.team01.freelance.contract.support.ContractJdbcFixtures.insertJob;
import static com.team01.freelance.contract.support.ContractJdbcFixtures.insertUser;
import static com.team01.freelance.contract.support.ContractJdbcFixtures.saveContract;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [S4-F9] Integration tests for {@code GET /api/contracts/stalled}.
 */
@Transactional
class ContractF9IntegrationTest extends AbstractIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ContractService contractService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void stalledContracts_returnsLowProgressContracts() throws Exception {
        insertUser(jdbcTemplate, 951L, "Freelancer");
        insertUser(jdbcTemplate, 952L, "Client");
        insertJob(jdbcTemplate, 851L, 952L, "Stalled Job");
        Map<String, Object> stalledMetadata = Map.of(
                "progressPercentage", 10,
                "lastActivityDate", LocalDateTime.now().minusDays(14).toString());
        saveContract(jdbcTemplate, 7501L, 851L, 951L, 952L, 1000.0, ContractStatus.ACTIVE,
                LocalDateTime.now().minusDays(20),
                LocalDateTime.now().minusDays(20), null, stalledMetadata);

        assertEquals(1, contractService.findStalledContracts(20.0, 7).size());

        mockMvc.perform(get("/api/contracts/stalled")
                        .param("maxProgress", "20")
                        .param("stalledDays", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].contractId").value(7501));
    }
}
