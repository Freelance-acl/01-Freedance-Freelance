package com.team01.freelance.contract.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team01.freelance.contract.model.ContractStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * JDBC helpers for contract integration tests (H2 shared schema).
 */
public final class ContractJdbcFixtures {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ContractJdbcFixtures() {
    }

    public static void insertUser(JdbcTemplate jdbcTemplate, Long id, String name) {
        jdbcTemplate.update("""
                        MERGE INTO users (id, created_at, email, name, password, phone, role, status, preferences)
                        KEY(id)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
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

    public static void insertJob(JdbcTemplate jdbcTemplate, Long id, Long clientId, String title) {
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

    public static void saveContract(
            JdbcTemplate jdbcTemplate,
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

    private static String toJson(Map<String, Object> metadata) {
        if (metadata == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize metadata for test fixture", e);
        }
    }
}
