package com.team01.freelance.contract.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Map;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class ContractF3IntegrationTest extends AbstractIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void searchContractsWithStatusFilterReturnsMatchingDto() throws Exception {
        insertUser(701L, "Freelancer A");
        insertUser(702L, "Freelancer B");
        insertJob(801L, 901L, "Small Job");
        insertJob(802L, 902L, "Large Job");

        saveContract(9101L, 801L, 701L, 901L, 1000.0, ContractStatus.ACTIVE,
                LocalDateTime.now().minusDays(3), LocalDateTime.of(2026, 3, 1, 9, 0), null, null);
        saveContract(9102L, 802L, 702L, 902L, 5000.0, ContractStatus.ACTIVE,
                LocalDateTime.now().minusDays(2), LocalDateTime.of(2026, 3, 2, 9, 0), null, null);
        saveContract(9103L, 803L, 702L, 902L, 3000.0, ContractStatus.COMPLETED,
                LocalDateTime.of(2026, 3, 1, 9, 0), LocalDateTime.of(2026, 3, 3, 9, 0),
                LocalDateTime.of(2026, 3, 5, 9, 0),
                null);

        mockMvc.perform(get("/api/contracts/search")
                        .param("minAmount", "2000")
                        .param("maxAmount", "6000")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].contractId").value(9102))
                .andExpect(jsonPath("$[0].freelancerName").value("Freelancer B"))
                .andExpect(jsonPath("$[0].jobTitle").value("Large Job"))
                .andExpect(jsonPath("$[0].agreedAmount").value(5000.0))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].durationDays", greaterThanOrEqualTo(2)));
    }

    @Test
    void searchContractsWithoutStatusFilterReturnsAmountSortedResults() throws Exception {
        insertUser(703L, "Freelancer C");
        insertUser(704L, "Freelancer D");
        insertJob(803L, 903L, "API Refactor");
        insertJob(804L, 904L, "Data Cleanup");

        saveContract(9201L, 803L, 703L, 903L, 1000.0, ContractStatus.ACTIVE,
                LocalDateTime.of(2026, 3, 1, 9, 0), LocalDateTime.of(2026, 3, 2, 9, 0), null, null);
        saveContract(9202L, 804L, 704L, 904L, 3000.0, ContractStatus.COMPLETED,
                LocalDateTime.of(2026, 3, 1, 9, 0), LocalDateTime.of(2026, 3, 10, 9, 0),
                LocalDateTime.of(2026, 3, 12, 9, 0),
                null);
        saveContract(9203L, 805L, 704L, 904L, 5000.0, ContractStatus.ACTIVE,
                LocalDateTime.of(2026, 3, 1, 9, 0), LocalDateTime.of(2026, 3, 15, 9, 0), null, null);

        mockMvc.perform(get("/api/contracts/search")
                        .param("minAmount", "500")
                        .param("maxAmount", "4000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].contractId", contains(9202, 9201)));
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

    private void insertJob(Long id, Long clientId, String title) {
        jdbcTemplate.update("""
                        MERGE INTO jobs (
                            id, client_id, title, description, category, status,
                            budget_min, budget_max, rating, total_ratings, requirements, created_at
                        )
                        KEY(id)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, cast(? as jsonb), ?)
                        """,
                id,
                clientId,
                title,
                "seeded job",
                "WEB_DEV",
                "OPEN",
                500.0,
                6000.0,
                0.0,
                0,
                null,
                LocalDateTime.of(2026, 3, 1, 9, 0)
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
            LocalDateTime endDate,
            Map<String, Object> metadata
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
                toJson(metadata),
                createdAt
        );
    }

    private String toJson(Map<String, Object> metadata) {
        if (metadata == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize metadata for test fixture", e);
        }
    }
}
