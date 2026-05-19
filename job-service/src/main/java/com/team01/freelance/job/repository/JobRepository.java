package com.team01.freelance.job.repository;

import com.team01.freelance.job.model.Job;
import com.team01.freelance.job.dto.TopBudgetJobDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

	@Query(value = "SELECT DISTINCT j.* FROM jobs j JOIN job_attachments ja ON ja.job_id = j.id WHERE ja.expiry_date < CURRENT_DATE ORDER BY j.id", nativeQuery = true)
	List<Job> findJobsWithExpiredAttachments();

	/**
	 * Searches jobs by a key-value pair in the requirements JSONB column.
	 * Filters by status if provided.
	 *
	 * @param key the JSON key to search for
	 * @param value the value to match
	 * @param status the job status filter (optional)
	 * @return a list of jobs matching the criteria
	 */
	@Query(value = "SELECT j.* FROM jobs j " +
			"WHERE j.requirements #>> ARRAY[:key] = :value " +
			"AND (:status IS NULL OR j.status = CAST(:status AS VARCHAR))",
			nativeQuery = true)
	List<Job> searchByRequirements(@Param("key") String key, @Param("value") String value, @Param("status") String status);

	/**
	 * Searches jobs by a key-value pair in the requirements JSONB column.
	 * Returns all jobs regardless of status.
	 *
	 * @param key the JSON key to search for
	 * @param value the value to match
	 * @return a list of jobs matching the criteria
	 */
	@Query(value = "SELECT j.* FROM jobs j " +
			"WHERE j.requirements #>> ARRAY[:key] = :value",
			nativeQuery = true)
	List<Job> searchByRequirements(@Param("key") String key, @Param("value") String value);

	/**
	 * Atomically closes a job only when no ACTIVE contract exists for it.
	 *
	 * @return number of rows updated (1 = closed, 0 = blocked or already closed)
	 */
	@Modifying(clearAutomatically = true)
	@Query(value = """
			UPDATE jobs
			SET status = 'CLOSED'
			WHERE id = :jobId
			  AND status <> 'CLOSED'
			  AND NOT EXISTS (
			    SELECT 1 FROM contracts c
			    WHERE c.job_id = :jobId AND c.status = 'ACTIVE'
			  )
			""", nativeQuery = true)
	int closeJobIfEligible(@Param("jobId") Long jobId);

	@Modifying(clearAutomatically = true)
	@Query(value = """
			UPDATE proposals
			SET status = 'REJECTED'
			WHERE job_id = :jobId AND status = 'SUBMITTED'
			""", nativeQuery = true)
	int rejectSubmittedProposalsByJobId(@Param("jobId") Long jobId);

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

