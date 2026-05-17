package com.team01.freelance.contract.repository;

import com.team01.freelance.contract.model.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {
    @Query(value = "SELECT COUNT(*) > 0 FROM users WHERE id = :userId", nativeQuery = true)
    boolean userExists(@Param("userId") Long userId);

    @Query(value = """
            SELECT *
            FROM contracts
            WHERE status = 'ACTIVE'
              AND (freelancer_id = :userId OR client_id = :userId)
            ORDER BY created_at DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<Contract> findMostRecentActiveContractForUser(@Param("userId") Long userId);

    @Query(value = """
            SELECT COUNT(*)
            FROM contracts c
            WHERE c.created_at < :cutoff
              AND c.status IN ('COMPLETED', 'TERMINATED')
            """, nativeQuery = true)
    long countPurgeCandidates(@Param("cutoff") LocalDateTime cutoff);

    @Modifying
    @Query(value = """
            DELETE FROM contracts
            WHERE created_at < :cutoff
              AND status IN ('COMPLETED', 'TERMINATED')
            """, nativeQuery = true)
    int purgeOldContracts(@Param("cutoff") LocalDateTime cutoff);

    @Query(value = "SELECT COUNT(*) > 0 FROM users WHERE id = :freelancerId", nativeQuery = true)
    boolean freelancerExists(@Param("freelancerId") Long freelancerId);

    @Query(value = """
            SELECT
                CAST(COUNT(*) AS BIGINT) AS total_contracts,
                CAST(SUM(CASE WHEN c.status = 'COMPLETED' THEN 1 ELSE 0 END) AS BIGINT) AS completed_contracts,
                COALESCE(SUM(CASE WHEN c.status = 'COMPLETED' THEN c.agreed_amount ELSE 0 END), 0) AS total_earnings,
                CASE
                    WHEN COUNT(*) = 0 THEN 0
                    ELSE COALESCE(SUM(CASE WHEN c.status = 'COMPLETED' THEN c.agreed_amount ELSE 0 END), 0) / COUNT(*)
                END AS average_contract_value,
                COALESCE(
                    AVG(
                        CASE
                            WHEN c.status = 'COMPLETED' AND c.end_date IS NOT NULL
                            THEN EXTRACT(EPOCH FROM (c.end_date - c.start_date)) / 86400.0
                            ELSE NULL
                        END
                    ),
                    0
                ) AS average_duration_days
            FROM contracts c
            WHERE c.freelancer_id = :freelancerId
              AND c.created_at >= :startDate
              AND c.created_at < :endDateExclusive
            """, nativeQuery = true)
    Object[] getFreelancerPerformance(
            @Param("freelancerId") Long freelancerId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDateExclusive") LocalDateTime endDateExclusive
    );

    @Query(value = """
            SELECT
                c.id AS contract_id,
                COALESCE(u.name, 'Unknown Freelancer') AS freelancer_name,
                COALESCE(j.title, 'Unknown Job') AS job_title,
                c.agreed_amount,
                CAST(COALESCE(c.metadata ->> 'progressPercentage', '0') AS DOUBLE PRECISION) AS progress_percentage,
                CAST(
                    EXTRACT(
                        DAY FROM (
                            CURRENT_TIMESTAMP - COALESCE(
                                CAST(c.metadata ->> 'lastActivityDate' AS TIMESTAMP),
                                c.created_at
                            )
                        )
                    ) AS BIGINT
                ) AS days_since_last_activity
            FROM contracts c
            LEFT JOIN users u ON u.id = c.freelancer_id
            LEFT JOIN jobs j ON j.id = c.job_id
            WHERE c.status = 'ACTIVE'
              AND CAST(COALESCE(c.metadata ->> 'progressPercentage', '0') AS DOUBLE PRECISION) <= :maxProgress
              AND COALESCE(
                    CAST(c.metadata ->> 'lastActivityDate' AS TIMESTAMP),
                    c.created_at
                  ) < (CURRENT_TIMESTAMP - (:stalledDays * INTERVAL '1 day'))
            ORDER BY days_since_last_activity DESC
            """, nativeQuery = true)
    List<Object[]> findStalledContracts(
            @Param("maxProgress") Double maxProgress,
            @Param("stalledDays") Integer stalledDays
    );
}
