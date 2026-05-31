package com.team01.freelance.wallet.repository;

import com.team01.freelance.wallet.model.Payout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.team01.freelance.wallet.model.PayoutStatus;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;


import java.util.List;
import java.util.Optional;

@Repository
public interface PayoutRepository extends JpaRepository<Payout, Long> {
    @Query("""
        SELECT p FROM Payout p
        LEFT JOIN FETCH p.payoutPromos pp
        LEFT JOIN FETCH pp.promoCode
        WHERE p.id = :id
    """)
    Optional<Payout> findByIdWithPromos(Long id);

    @Query(value = """
        SELECT 
            pc.id AS promoCodeId,
            pc.code AS code,
            pc.discount_type AS discountType,
            pc.discount_value AS discountValue,
            pc.current_uses AS timesUsed,
            COALESCE(SUM(pp.discount_applied), 0) AS totalDiscountGiven,
            pc.active AS active,
            pc.expiry_date AS expiryDate
        FROM promo_codes pc
        LEFT JOIN payout_promos pp ON pp.promo_code_id = pc.id
        GROUP BY pc.id, pc.code, pc.discount_type, pc.discount_value, pc.current_uses, pc.active, pc.expiry_date
        ORDER BY pc.current_uses DESC
        LIMIT :limit
    """, nativeQuery = true)
    List<Object[]> findTopUsedPromoCodes(@Param("limit") int limit); // Verify after prerequisite task completion

    // Uses native SQL with explicit CASTs so PostgreSQL can resolve the
    // parameter types when they appear inside `IS NULL` checks. Status is
    // stored as VARCHAR by @Enumerated(EnumType.STRING), so we compare it
    // against the enum name passed in as text.
    //
    // Date range is half-open: [startDate, endExclusive). Callers should
    // pass endExclusive = (userEndDate + 1 day) at start-of-day so the
    // upper bound is precision-agnostic (no LocalTime.MAX nano fiddling).
    @Query(value = """
            SELECT *
            FROM payouts
            WHERE (CAST(:status AS text) IS NULL OR status = CAST(:status AS text))
              AND (CAST(:startDate    AS timestamp) IS NULL OR created_at >= CAST(:startDate    AS timestamp))
              AND (CAST(:endExclusive AS timestamp) IS NULL OR created_at <  CAST(:endExclusive AS timestamp))
            ORDER BY created_at DESC
            """, nativeQuery = true)
    List<Payout> searchByStatusAndCreatedAtRange(
            @Param("status") String status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endExclusive") LocalDateTime endExclusive
    );

    /**
     * [S5-F3] Aggregate COMPLETED payouts for a freelancer, grouped by method.
     * Each row is: [method (String), count (Long), totalAmount (Double)].
     */
    @Query(value = """
            SELECT method,
                   COUNT(id)            AS payout_count,
                   COALESCE(SUM(amount), 0) AS total_amount
            FROM payouts
            WHERE freelancer_id = :freelancerId
              AND status = 'COMPLETED'
            GROUP BY method
            ORDER BY method
            """, nativeQuery = true)
    List<Object[]> aggregateCompletedByMethodForFreelancer(@Param("freelancerId") Long freelancerId);

    Optional<Payout> findByContractIdAndStatus(Long contractId, PayoutStatus status);

    boolean existsByContractIdAndStatus(Long contractId, PayoutStatus status);

    /**
     * Loads a payout row with a row-level write lock (FOR UPDATE). Uses native SQL so
     * H2 (tests) and PostgreSQL (runtime) both accept the lock clause.
     */
    @Query(value = """
            SELECT *
            FROM payouts
            WHERE contract_id = :contractId
              AND status = :status
            FOR UPDATE
            """, nativeQuery = true)
    Optional<Payout> findByContractIdAndStatusForUpdate(
            @Param("contractId") Long contractId,
            @Param("status") String status);

    @Query(value = "SELECT COUNT(*) FROM contracts WHERE id = :id", nativeQuery = true)
    Long countContractById(@Param("id") Long id);

    @Query(value = "SELECT status FROM contracts WHERE id = :id", nativeQuery = true)
    String findContractStatusById(@Param("id") Long id);

    @Query(value = "SELECT COUNT(*) FROM users WHERE id = :id", nativeQuery = true)
    Long countUserById(@Param("id") Long id);

    @Query(value = "SELECT COALESCE(SUM(amount), 0) FROM payouts WHERE status = 'COMPLETED' AND created_at BETWEEN :start AND :end", nativeQuery = true)
    Double sumCompletedAmountBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = "SELECT COUNT(*) FROM payouts WHERE status = 'COMPLETED' AND created_at BETWEEN :start AND :end", nativeQuery = true)
    Long countCompletedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = "SELECT COALESCE(SUM(amount), 0) FROM payouts WHERE status = 'REFUNDED' AND created_at BETWEEN :start AND :end", nativeQuery = true)
    Double sumRefundedAmountBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = "SELECT COUNT(*) FROM payouts WHERE status = 'REFUNDED' AND created_at BETWEEN :start AND :end", nativeQuery = true)
    Long countRefundedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * [S5-F10] Aggregate COMPLETED payouts by job category, joining contracts and jobs tables.
     * Each row: [category (String), platformFeeRevenue (Double), netPayoutRevenue (Double),
     *            totalRevenue (Double), payoutCount (Long)]
     * platformFeeRevenue uses transactionDetails->>'platformFee' cast to numeric,
     * falling back to amount * 0.10 when the key is absent.
     */
    @Query(value = """
            SELECT j.category                                                               AS category,
                   SUM(COALESCE(
                       NULLIF(p.transaction_details->>'platformFee', '')::numeric,
                       p.amount * 0.10
                   ))                                                                       AS platform_fee_revenue,
                   SUM(p.amount) - SUM(COALESCE(
                       NULLIF(p.transaction_details->>'platformFee', '')::numeric,
                       p.amount * 0.10
                   ))                                                                       AS net_payout_revenue,
                   SUM(p.amount)                                                            AS total_revenue,
                   COUNT(p.id)                                                              AS payout_count
            FROM payouts p
            JOIN contracts c ON c.id = p.contract_id
            JOIN jobs j      ON j.id = c.job_id
            WHERE p.status = 'COMPLETED'
              AND p.created_at >= :start
              AND p.created_at <= :end
            GROUP BY j.category
            ORDER BY j.category
            """, nativeQuery = true)
    List<Object[]> aggregateCompletedByCategory(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Modifying(clearAutomatically = true)
    @Query(value = """
            INSERT INTO payouts (
                contract_id, freelancer_id, amount, method, status, created_at
            )
            VALUES (
                :contractId, :freelancerId, :amount, 'BANK_TRANSFER', 'PENDING', :createdAt
            )
            """, nativeQuery = true)
    void insertPendingPayout(
            @Param("contractId") Long contractId,
            @Param("freelancerId") Long freelancerId,
            @Param("amount") Double amount,
            @Param("createdAt") LocalDateTime createdAt);

    @Query(value = """
            SELECT COALESCE(SUM(pm.amount), 0)
            FROM contracts c
            JOIN proposal_milestones pm ON pm.proposal_id = c.proposal_id
            WHERE c.id = :contractId
              AND pm.status NOT IN ('COMPLETED', 'APPROVED')
            """, nativeQuery = true)
    Double sumIncompleteMilestoneAmounts(@Param("contractId") Long contractId);

}

