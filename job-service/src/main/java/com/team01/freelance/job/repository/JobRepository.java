package com.team01.freelance.job.repository;

import com.team01.freelance.job.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    @Query(value = "SELECT * FROM jobs "
            + "WHERE (:status IS NULL OR status = :status) "
            + "AND (budget_max >= :minBudget AND budget_min <= :maxBudget) "
            + "ORDER BY budget_max DESC",
            countQuery = "SELECT count(*) FROM jobs "
                    + "WHERE (:status IS NULL OR status = :status) "
                    + "AND (budget_max >= :minBudget AND budget_min <= :maxBudget)",
            nativeQuery = true)
    Page<Job> searchJobsByStatusAndBudgetRange(
            @Param("status") String status,
            @Param("minBudget") Double minBudget,
            @Param("maxBudget") Double maxBudget,
            Pageable pageable);
}

