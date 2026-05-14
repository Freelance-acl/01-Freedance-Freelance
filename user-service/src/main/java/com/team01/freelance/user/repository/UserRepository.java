package com.team01.freelance.user.repository;

import com.team01.freelance.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @Query(value = """
            SELECT *
            FROM users
            WHERE jsonb_extract_path_text(preferences, :key) = :value
            """, nativeQuery = true)
    List<User> findByPreference(@Param("key") String key, @Param("value") String value);
}

