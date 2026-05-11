package com.team01.freelance.job.repository;

import com.team01.freelance.job.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    @Query(value = "SELECT * FROM jobs "
            + "WHERE (:status IS NULL OR status = :status) "
            + "AND budget_max BETWEEN :minBudget AND :maxBudget "
            + "ORDER BY budget_max DESC",
            nativeQuery = true)
    List<Job> searchJobsByStatusAndBudgetRange(
            @Param("status") String status,
            @Param("minBudget") Double minBudget,
            @Param("maxBudget") Double maxBudget);
}

