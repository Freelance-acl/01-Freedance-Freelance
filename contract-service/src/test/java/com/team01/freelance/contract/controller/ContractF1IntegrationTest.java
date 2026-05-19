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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class ContractF1IntegrationTest extends AbstractIntegrationTest {

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
    void getActiveContractForUserReturnsMostRecentActiveContract() throws Exception {
        insertUser(101L, "Mariam");
        insertUser(201L, "Client");

        saveContract(9001L, 301L, 101L, 201L, 1000.0, ContractStatus.ACTIVE,
                LocalDateTime.of(2026, 3, 1, 9, 0),
                LocalDateTime.of(2026, 3, 10, 9, 0),
                null);
        saveContract(9002L, 302L, 101L, 201L, 2000.0, ContractStatus.ACTIVE,
                LocalDateTime.of(2026, 3, 2, 9, 0),
                LocalDateTime.of(2026, 3, 15, 9, 0),
                null);
        saveContract(9003L, 303L, 101L, 201L, 3000.0, ContractStatus.ACTIVE,
                LocalDateTime.of(2026, 3, 3, 9, 0),
                LocalDateTime.of(2026, 3, 20, 9, 0),
                null);

        mockMvc.perform(get("/api/contracts/user/{userId}/active", 101L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9003))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getActiveContractForUserWithoutActiveContractReturns404() throws Exception {
        insertUser(102L, "No Active");
        insertUser(202L, "Client");

        saveContract(9010L, 310L, 102L, 202L, 800.0, ContractStatus.COMPLETED,
                LocalDateTime.of(2026, 3, 1, 9, 0),
                LocalDateTime.of(2026, 3, 5, 9, 0),
                LocalDateTime.of(2026, 3, 6, 9, 0));

        mockMvc.perform(get("/api/contracts/user/{userId}/active", 102L))
                .andExpect(status().isNotFound());
    }

    @Test
    void getActiveContractForUserUnknownUserReturns404() throws Exception {
        mockMvc.perform(get("/api/contracts/user/{userId}/active", 999999L))
                .andExpect(status().isNotFound());
    }

    private void insertUser(Long id, String name) {
        jdbcTemplate.update("""
                        MERGE INTO users (id, created_at, email, name, password, phone, role, status, preferences)
                        KEY(id)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, cast(? as jsonb))
                        """,
                id,
                LocalDateTime.of(2026, 3, 1, 9, 0),
                "user-" + id + "@test.dev",
                name,
                "secret",
                "+1000" + id,
                "FREELANCER",
                "ACTIVE",
                null
        );
    }

    private void saveContract(
            Long id,
            Long jobId,
            Long freelancerId,
            Long clientId,
            Double agreedAmount,
            ContractStatus status,
            LocalDateTime createdAt,
            LocalDateTime startDate,
            LocalDateTime endDate
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
                jobId + 1000,
                agreedAmount,
                status.name(),
                startDate,
                endDate,
                null,
                createdAt
        );
    }
}
