package com.team01.freelance.user.repository;

import com.team01.freelance.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @Query(value = """
            SELECT COUNT(*)
            FROM contracts
            WHERE status = 'ACTIVE'
              AND (freelancer_id = :userId OR client_id = :userId)
            """, nativeQuery = true)
    long countActiveContractsForUser(@Param("userId") Long userId);

    @Modifying
    @Query(value = """
            UPDATE proposals
            SET status = 'WITHDRAWN'
            WHERE freelancer_id = :userId
              AND status = 'SUBMITTED'
            """, nativeQuery = true)
    int withdrawSubmittedProposalsForUser(@Param("userId") Long userId);
}
