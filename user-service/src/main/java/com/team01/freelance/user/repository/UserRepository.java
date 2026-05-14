package com.team01.freelance.user.repository;

import com.team01.freelance.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.team01.freelance.user.model.UserRole;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @Query(value = """
        SELECT
            COUNT(c.id) AS total_contracts,
            COALESCE(SUM(CASE WHEN c.status = 'COMPLETED' THEN 1 ELSE 0 END), 0) AS completed_contracts,
            COALESCE(SUM(CASE WHEN c.status = 'TERMINATED' THEN 1 ELSE 0 END), 0) AS terminated_contracts,
            COALESCE(SUM(CASE WHEN c.status = 'COMPLETED' THEN c.agreed_amount ELSE 0 END), 0) AS total_earnings,
            COALESCE(AVG(CASE WHEN c.status = 'COMPLETED' THEN c.agreed_amount END), 0) AS average_contract_value
        FROM contracts c
        WHERE c.freelancer_id = :userId OR c.client_id = :userId
        """, nativeQuery = true)
    Object getUserContractSummary(@Param("userId") Long userId);
}

