package com.team01.freelance.wallet.repository;

import com.team01.freelance.wallet.model.Payout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}

