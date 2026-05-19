package com.team01.freelance.contract.controller;

import com.team01.freelance.contract.model.ContractStatus;
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

import static com.team01.freelance.contract.support.ContractJdbcFixtures.insertJob;
import static com.team01.freelance.contract.support.ContractJdbcFixtures.insertUser;
import static com.team01.freelance.contract.support.ContractJdbcFixtures.saveContract;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [S4-F7] Integration tests for {@code DELETE /api/contracts/purge}.
 */
@Transactional
class ContractF7IntegrationTest extends AbstractIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void purgeOldContracts_returnsDeletedCount() throws Exception {
        insertUser(jdbcTemplate, 931L, "Freelancer");
        insertUser(jdbcTemplate, 932L, "Client");
        insertJob(jdbcTemplate, 831L, 932L, "Purge Job");
        saveContract(jdbcTemplate, 7301L, 831L, 931L, 932L, 500.0, ContractStatus.COMPLETED,
                LocalDateTime.now().minusDays(60),
                LocalDateTime.now().minusDays(60),
                LocalDateTime.now().minusDays(45), null);

        mockMvc.perform(delete("/api/contracts/purge").param("olderThanDays", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedCount").value(1));
    }
}
