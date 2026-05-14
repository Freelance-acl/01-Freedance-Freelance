package com.team01.freelance.wallet.repository;

import com.team01.freelance.wallet.model.Payout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;


@Repository
public interface PayoutRepository extends JpaRepository<Payout, Long> {

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
}

