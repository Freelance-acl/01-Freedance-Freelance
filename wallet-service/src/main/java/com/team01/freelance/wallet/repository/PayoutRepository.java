package com.team01.freelance.wallet.repository;

import com.team01.freelance.wallet.model.Payout;
import com.team01.freelance.wallet.model.PayoutStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PayoutRepository extends JpaRepository<Payout, Long> {
    Optional<Payout> findByContractIdAndStatus(Long contractId, PayoutStatus status);
    boolean existsByContractIdAndStatus(Long contractId, PayoutStatus status);

    @Query(value = "SELECT COUNT(*) FROM contracts WHERE id = :id", nativeQuery = true)
    Long countContractById(@Param("id") Long id);

    @Query(value = "SELECT status FROM contracts WHERE id = :id", nativeQuery = true)
    String findContractStatusById(@Param("id") Long id);

    @Query(value = "SELECT COUNT(*) FROM users WHERE id = :id", nativeQuery = true)
    Long countUserById(@Param("id") Long id);
}

