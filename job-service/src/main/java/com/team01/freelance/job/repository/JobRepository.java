package com.team01.freelance.job.repository;

import com.team01.freelance.job.dto.JobProposalSummaryDTO;
import com.team01.freelance.job.dto.TopBudgetJobDTO;
import com.team01.freelance.job.model.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    /**
     * Aggregates proposal statistics for a job within a date range.
     * Returns job details and aggregated proposal metrics (count, avg, min, max bid amounts).
     *
     * @param jobId the job ID
     * @param startDate inclusive start of date range (start of day)
     * @param endDateExclusive exclusive end of date range (start of next day)
     * @return Optional containing JobProposalSummaryDTO with aggregated proposal data,
     *         or empty if job does not exist
     */
    @Query(value = """
            SELECT
                j.id AS jobId,
                j.title,
                COUNT(p.id) AS totalProposals,
                COALESCE(AVG(p.bid_amount), 0) AS averageBidAmount,
                COALESCE(MIN(p.bid_amount), 0) AS lowestBid,
                COALESCE(MAX(p.bid_amount), 0) AS highestBid
            FROM jobs j
            LEFT JOIN proposals p ON j.id = p.job_id
              AND p.submitted_at >= :startDate
              AND p.submitted_at < :endDateExclusive
            WHERE j.id = :jobId
            GROUP BY j.id, j.title
            """, nativeQuery = true)
    List<Object[]> getProposalSummary(
            @Param("jobId") Long jobId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDateExclusive") LocalDateTime endDateExclusive
    );

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

	@Modifying(clearAutomatically = true)
	@Query(value = "UPDATE jobs SET status = 'IN_PROGRESS' WHERE id = :jobId", nativeQuery = true)
	int markJobInProgress(@Param("jobId") Long jobId);

	@Modifying(clearAutomatically = true)
	@Query(value = "UPDATE jobs SET status = 'CLOSED' WHERE id = :jobId", nativeQuery = true)
	int markJobClosed(@Param("jobId") Long jobId);

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


	@Modifying
	@Query(value = "UPDATE jobs SET status = 'OPEN' WHERE id = :jobId AND status = 'IN_PROGRESS'", nativeQuery = true)
	int reopenIfInProgress(@Param("jobId") Long jobId);

	/**
	 * Aggregated dashboard query returning basic metrics per job.
	 * Columns: jobId, title, totalProposals, averageBidAmount, activeAttachments, rating
	 */
	@Query(value = """
			SELECT j.id as jobId, j.title,
				   COALESCE(COUNT(p.id), 0) as totalProposals,
				   COALESCE(AVG(p.bid_amount), 0) as averageBidAmount,
				   COALESCE(SUM(CASE WHEN ja.expiry_date >= CURRENT_DATE THEN 1 ELSE 0 END), 0) as activeAttachments,
				   COALESCE(j.rating, 0) as rating
			FROM jobs j
			LEFT JOIN proposals p ON p.job_id = j.id
			LEFT JOIN job_attachments ja ON ja.job_id = j.id
			GROUP BY j.id, j.title, j.rating
			""", nativeQuery = true)
	List<Object[]> findJobDashboard();
}
