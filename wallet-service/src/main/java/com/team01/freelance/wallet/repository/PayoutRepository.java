package com.team01.freelance.wallet.repository;

import com.team01.freelance.wallet.model.Payout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PayoutRepository extends JpaRepository<Payout, Long> {

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
