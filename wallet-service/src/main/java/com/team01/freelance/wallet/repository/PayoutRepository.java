package com.team01.freelance.wallet.repository;

import com.team01.freelance.wallet.model.Payout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface PayoutRepository extends JpaRepository<Payout, Long> {

    // Cross-service existence checks (avoids importing classes from other modules)
    @Query(value = "SELECT COUNT(*) FROM contracts WHERE id = :id", nativeQuery = true)
    Long countContractById(@Param("id") Long id);

    @Query(value = "SELECT COUNT(*) FROM users WHERE id = :id", nativeQuery = true)
    Long countUserById(@Param("id") Long id);

    // S5-F6: Revenue report aggregations
    @Query(value = "SELECT COALESCE(SUM(amount), 0) FROM payouts WHERE status = 'COMPLETED' AND created_at BETWEEN :start AND :end", nativeQuery = true)
    Double sumCompletedAmountBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = "SELECT COUNT(*) FROM payouts WHERE status = 'COMPLETED' AND created_at BETWEEN :start AND :end", nativeQuery = true)
    Long countCompletedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = "SELECT COALESCE(SUM(amount), 0) FROM payouts WHERE status = 'REFUNDED' AND created_at BETWEEN :start AND :end", nativeQuery = true)
    Double sumRefundedAmountBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = "SELECT COUNT(*) FROM payouts WHERE status = 'REFUNDED' AND created_at BETWEEN :start AND :end", nativeQuery = true)
    Long countRefundedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
