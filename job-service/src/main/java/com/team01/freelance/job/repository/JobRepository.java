package com.team01.freelance.job.repository;

import com.team01.freelance.job.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
}

