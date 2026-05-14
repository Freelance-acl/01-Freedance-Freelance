package com.team01.freelance.user.repository;

import com.team01.freelance.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
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
}

