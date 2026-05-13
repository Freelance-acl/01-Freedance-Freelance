package com.team01.freelance.wallet.repository;

import com.team01.freelance.wallet.model.Payout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

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
}

