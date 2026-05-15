package com.team01.freelance.job.repository;

import com.team01.freelance.job.dto.TopBudgetJobDTO;
import com.team01.freelance.job.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    /**
     * Retrieves the top jobs ordered by budgetMax descending.
     * Includes the count of proposals for each job.
     * Uses native SQL to join with the proposals table.
     *
     * @param limit the maximum number of jobs to return
     * @return a list of TopBudgetJobDTO with job details and proposal counts
     */
    @Query(value = "SELECT j.id as jobId, j.title, j.budget_max as budgetMax, " +
            "COALESCE(COUNT(p.id), 0) as totalProposals " +
            "FROM jobs j " +
            "LEFT JOIN proposals p ON j.id = p.job_id " +
            "GROUP BY j.id, j.title, j.budget_max " +
            "ORDER BY j.budget_max DESC " +
            "LIMIT :limit", 
            nativeQuery = true)
    List<TopBudgetJobDTO> findTopBudgetJobs(@Param("limit") int limit);
}
}

