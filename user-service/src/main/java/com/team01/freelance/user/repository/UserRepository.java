package com.team01.freelance.user.repository;

import com.team01.freelance.user.model.User;
import com.team01.freelance.user.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

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
}