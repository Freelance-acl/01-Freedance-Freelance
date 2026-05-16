package com.team01.freelance.user.repository;

import com.team01.freelance.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import com.team01.freelance.user.model.UserRole;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("""
            SELECT u FROM User u
            WHERE (:name IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :name, '%')))
              AND (:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%')))
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
        SELECT u.*
        FROM users u
        LEFT JOIN contracts c
            ON (c.freelancer_id = u.id OR c.client_id = u.id)
            AND c.status = 'COMPLETED'
        WHERE LOWER(u.preferences ->> 'language') = LOWER(:lang)
        GROUP BY u.id
        HAVING COUNT(c.id) >= :minContracts
        """, nativeQuery = true)
    List<User> findUsersByLanguageAndMinimumCompletedContracts(
            @Param("lang") String lang,
            @Param("minContracts") Long minContracts
    );


}

