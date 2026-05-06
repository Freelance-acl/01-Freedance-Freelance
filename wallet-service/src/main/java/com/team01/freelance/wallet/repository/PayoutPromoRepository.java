package com.team01.freelance.wallet.repository;

import com.team01.freelance.wallet.model.PayoutPromo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PayoutPromoRepository extends JpaRepository<PayoutPromo, Long> {
}

