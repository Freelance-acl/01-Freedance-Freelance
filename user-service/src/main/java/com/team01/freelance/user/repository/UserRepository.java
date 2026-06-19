package com.team01.freelance.user.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.team01.freelance.user.model.UserRole;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;

import com.team01.freelance.user.model.User;
import com.team01.freelance.user.model.UserRole;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

        Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("""
            SELECT u FROM User u
            WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', COALESCE(:name, ''), '%'))
              AND LOWER(u.email) LIKE LOWER(CONCAT('%', COALESCE(:email, ''), '%'))
              AND (:role IS NULL OR u.role = :role)
            """)
    List<User> searchUsers(
            @Param("name") String name,
            @Param("email") String email,
            @Param("role") UserRole role
    );

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

    @Query(value = """
            SELECT COUNT(*)
            FROM contracts
            WHERE status = 'ACTIVE'
              AND (freelancer_id = :userId OR client_id = :userId)
            """, nativeQuery = true)
    long countActiveContractsForUser(@Param("userId") Long userId);

    @Query(value = "SELECT role FROM users WHERE id = :userId", nativeQuery = true)
    String findRoleByUserId(@Param("userId") Long userId);

    @Modifying
    @Query(value = """
            UPDATE proposals
            SET status = 'WITHDRAWN'
            WHERE freelancer_id = :userId
              AND status = 'SUBMITTED'
            """, nativeQuery = true)
    int withdrawSubmittedProposalsForUser(@Param("userId") Long userId);

    @Query(value = """
            SELECT *
            FROM users
            WHERE jsonb_extract_path_text(preferences, :key) = :value
            """, nativeQuery = true)
    List<User> findByPreference(@Param("key") String key, @Param("value") String value);
    @Query(value = """
            SELECT u.id,
                   u.name,
                   COALESCE(SUM(c.agreed_amount), 0) AS total_earnings,
                   COUNT(c.id) AS contract_count
            FROM users u
            JOIN contracts c ON c.freelancer_id = u.id
            WHERE c.status = 'COMPLETED'
              AND c.end_date >= :startDate
              AND c.end_date <= :endDate
            GROUP BY u.id, u.name
            ORDER BY total_earnings DESC, u.id ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findTopFreelancersByEarnings(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("limit") int limit);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE users
            SET completed_contracts = COALESCE(completed_contracts, 0) + 1,
                total_earnings = COALESCE(total_earnings, 0) + :amount
            WHERE id = :freelancerId
            """, nativeQuery = true)
    int incrementFreelancerStats(
            @Param("freelancerId") Long freelancerId,
            @Param("amount") BigDecimal amount
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE users
            SET completed_contracts =
                    CASE
                        WHEN COALESCE(completed_contracts, 0) > 0
                        THEN COALESCE(completed_contracts, 0) - 1
                        ELSE 0
                    END,
                total_earnings =
                    CASE
                        WHEN COALESCE(total_earnings, 0) >= :amount
                        THEN COALESCE(total_earnings, 0) - :amount
                        ELSE 0
                    END
            WHERE id = :freelancerId
            """, nativeQuery = true)
    int decrementFreelancerStats(
            @Param("freelancerId") Long freelancerId,
            @Param("amount") BigDecimal amount
    );

}
