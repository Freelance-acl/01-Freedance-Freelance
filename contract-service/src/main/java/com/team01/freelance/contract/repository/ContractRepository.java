package com.team01.freelance.contract.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.team01.freelance.contract.model.Contract;
import com.team01.freelance.contract.model.ContractStatus;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {

    List<Contract> findByStatus(ContractStatus status);
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

    @Query(value = """
            SELECT *
            FROM contracts
            WHERE agreed_amount >= :minAmount
              AND agreed_amount <= :maxAmount
              AND (:status IS NULL OR status = :status)
            ORDER BY agreed_amount DESC, id DESC
            """, nativeQuery = true)
    List<Contract> searchContracts(
            @Param("minAmount") Double minAmount,
            @Param("maxAmount") Double maxAmount,
            @Param("status") String status
    );

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
            SELECT *
            FROM contracts c
            WHERE c.status = 'ACTIVE'
              AND CAST(COALESCE(c.metadata ->> 'progressPercentage', '0') AS DOUBLE PRECISION) <= :maxProgress
              AND COALESCE(
                    CAST(c.metadata ->> 'lastActivityDate' AS TIMESTAMP),
                    c.created_at
                  ) < (CURRENT_TIMESTAMP - (:stalledDays * INTERVAL '1 day'))
            ORDER BY COALESCE(CAST(c.metadata ->> 'lastActivityDate' AS TIMESTAMP), c.created_at) ASC
            """, nativeQuery = true)
    List<Contract> findStalledContracts(
            @Param("maxProgress") Double maxProgress,
            @Param("stalledDays") Integer stalledDays
    );

    List<Contract> findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAsc(
            LocalDateTime startDate,
            LocalDateTime endDateExclusive
    );

    List<Contract> findByCreatedAtGreaterThanEqualAndCreatedAtLessThanAndStatusOrderByCreatedAtAsc(
            LocalDateTime startDate,
            LocalDateTime endDateExclusive,
            com.team01.freelance.contract.model.ContractStatus status
    );
    @Query(value = """
            SELECT *
            FROM contracts
            WHERE metadata ->> :key = :value
            ORDER BY id ASC
            """, nativeQuery = true)
    List<Contract> findByMetadataEquals(@Param("key") String key, @Param("value") String value);

    @Query(value = """
            SELECT *
            FROM contracts
            WHERE CAST(metadata ->> :key AS DOUBLE PRECISION) > :value
            ORDER BY id ASC
            """, nativeQuery = true)
    List<Contract> findByMetadataGreaterThan(@Param("key") String key, @Param("value") Double value);

    @Query(value = """
            SELECT *
            FROM contracts
            WHERE CAST(metadata ->> :key AS DOUBLE PRECISION) < :value
            ORDER BY id ASC
            """, nativeQuery = true)
    List<Contract> findByMetadataLessThan(@Param("key") String key, @Param("value") Double value);

    Optional<Contract> findByProposalId(Long proposalId);

    @Query(value = """
            SELECT COUNT(*)
            FROM contracts
            WHERE freelancer_id = :userId OR client_id = :userId
            """, nativeQuery = true)
    long countContractsForUser(@Param("userId") Long userId);

    @Query(value = """
            SELECT COUNT(*)
            FROM contracts
            WHERE (freelancer_id = :userId OR client_id = :userId)
              AND status = 'ACTIVE'
            """, nativeQuery = true)
    int countActiveContractsForUser(@Param("userId") Long userId);

    @Query(value = """
            SELECT COUNT(*)
            FROM contracts
            WHERE (freelancer_id = :userId OR client_id = :userId)
              AND status = 'COMPLETED'
            """, nativeQuery = true)
    long countCompletedContractsForUser(@Param("userId") Long userId);

    @Query(value = """
            SELECT COUNT(*)
            FROM contracts
            WHERE (freelancer_id = :userId OR client_id = :userId)
              AND status = 'TERMINATED'
            """, nativeQuery = true)
    long countTerminatedContractsForUser(@Param("userId") Long userId);

    @Query(value = """
            SELECT COALESCE(SUM(agreed_amount), 0)
            FROM contracts
            WHERE (freelancer_id = :userId OR client_id = :userId)
              AND status = 'COMPLETED'
            """, nativeQuery = true)
    double sumCompletedContractEarningsForUser(@Param("userId") Long userId);

    @Query(value = """
            SELECT COUNT(*)
            FROM contracts
            WHERE job_id = :jobId
              AND status = 'ACTIVE'
            """, nativeQuery = true)
    int countActiveContractsForJob(@Param("jobId") Long jobId);

    @Modifying(clearAutomatically = true)
    @Query(value = """
            INSERT INTO contracts (
                job_id, freelancer_id, client_id, proposal_id,
                agreed_amount, status, start_date, created_at
            )
            VALUES (
                :jobId, :freelancerId, :clientId, :proposalId,
                :agreedAmount, 'ACTIVE', :timestamp, :timestamp
            )
            """, nativeQuery = true)
    void insertActiveContract(
            @Param("jobId") Long jobId,
            @Param("freelancerId") Long freelancerId,
            @Param("clientId") Long clientId,
            @Param("proposalId") Long proposalId,
            @Param("agreedAmount") Double agreedAmount,
            @Param("timestamp") LocalDateTime timestamp
    );

    @Query(value = """
            SELECT *
            FROM contracts
            WHERE proposal_id = :proposalId
              AND status = 'ACTIVE'
            LIMIT 1
            """, nativeQuery = true)
    Optional<Contract> findActiveContractByProposalId(@Param("proposalId") Long proposalId);

    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE contracts
            SET status = 'COMPLETED', end_date = :endDate
            WHERE id = :contractId
              AND status = 'ACTIVE'
            """, nativeQuery = true)
    int completeActiveContract(
            @Param("contractId") Long contractId,
            @Param("endDate") LocalDateTime endDate);

    @Query(value = """
            SELECT
                CAST(COUNT(*) AS BIGINT) AS total_contracts,
                COALESCE(AVG(agreed_amount), 0) AS avg_value,
                CASE
                    WHEN COUNT(*) = 0 THEN 0
                    ELSE SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) * 100.0 / COUNT(*)
                END AS completion_rate,
                COALESCE(
                    AVG(
                        CASE
                            WHEN end_date IS NOT NULL
                            THEN EXTRACT(EPOCH FROM (end_date - start_date)) / 86400.0
                            ELSE NULL
                        END
                    ),
                    0
                ) AS avg_duration_days
            FROM contracts
            """, nativeQuery = true)
    Object[] getContractAnalytics();

    @Query(value = """
            SELECT status, CAST(COUNT(*) AS BIGINT) AS contract_count
            FROM contracts
            GROUP BY status
            ORDER BY status ASC
            """, nativeQuery = true)
    List<Object[]> countContractsByStatus();

    @Query(value = """
            SELECT
                CAST(COUNT(*) AS BIGINT) AS total_contracts,
                COALESCE(AVG(agreed_amount), 0) AS avg_value,
                CASE
                    WHEN COUNT(*) = 0 THEN 0
                    ELSE SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) * 100.0 / COUNT(*)
                END AS completion_rate,
                COALESCE(
                    AVG(
                        CASE
                            WHEN end_date IS NOT NULL
                            THEN EXTRACT(EPOCH FROM (end_date - start_date)) / 86400.0
                            ELSE NULL
                        END
                    ),
                    0
                ) AS avg_duration_days
            FROM contracts
            WHERE start_date >= :start AND start_date < :endExclusive
            """, nativeQuery = true)
    Object[] getContractAnalyticsByDateRange(
            @Param("start") LocalDateTime start,
            @Param("endExclusive") LocalDateTime endExclusive);

    @Query(value = """
            SELECT status, CAST(COUNT(*) AS BIGINT) AS contract_count
            FROM contracts
            WHERE start_date >= :start AND start_date < :endExclusive
            GROUP BY status
            ORDER BY status ASC
            """, nativeQuery = true)
    List<Object[]> countContractsByStatusInDateRange(
            @Param("start") LocalDateTime start,
            @Param("endExclusive") LocalDateTime endExclusive);
}
