package com.team01.freelance.contract.controller;

import com.team01.freelance.contract.model.ContractStatus;
import com.team01.freelance.contract.support.AbstractIntegrationTest;
import com.team01.freelance.contract.support.ContractJdbcFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static com.team01.freelance.contract.support.ContractJdbcFixtures.insertJob;
import static com.team01.freelance.contract.support.ContractJdbcFixtures.insertUser;
import static com.team01.freelance.contract.support.ContractJdbcFixtures.saveContract;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [S4-F4] Integration tests for {@code PUT /api/contracts/batch-status}.
 */
@Transactional
class ContractF4IntegrationTest extends AbstractIntegrationTest {

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
    void batchStatusUpdate_completesActiveContracts() throws Exception {
        insertUser(jdbcTemplate, 901L, "Freelancer");
        insertUser(jdbcTemplate, 902L, "Client");
        insertJob(jdbcTemplate, 801L, 902L, "Batch Job");
        saveContract(jdbcTemplate, 7001L, 801L, 901L, 902L, 1000.0, ContractStatus.ACTIVE,
                LocalDateTime.now(), LocalDateTime.now(), null, null);
        saveContract(jdbcTemplate, 7002L, 801L, 901L, 902L, 1500.0, ContractStatus.ACTIVE,
                LocalDateTime.now(), LocalDateTime.now(), null, null);

        mockMvc.perform(put("/api/contracts/batch-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                  {"contractId":7001,"status":"COMPLETED"},
                                  {"contractId":7002,"status":"COMPLETED"}
                                ]
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));
    }
}
