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
    @Query(value = """
            SELECT *
            FROM payouts
            WHERE (CAST(:status AS text) IS NULL OR status = CAST(:status AS text))
              AND (CAST(:startDate AS timestamp) IS NULL OR created_at >= CAST(:startDate AS timestamp))
              AND (CAST(:endDate   AS timestamp) IS NULL OR created_at <= CAST(:endDate   AS timestamp))
            ORDER BY created_at DESC
            """, nativeQuery = true)
    List<Payout> searchByStatusAndCreatedAtRange(
            @Param("status") String status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}

