package com.team01.freelance.job.repository;

import com.team01.freelance.job.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

	@Query(value = "SELECT DISTINCT j.* FROM jobs j JOIN job_attachments ja ON ja.job_id = j.id WHERE ja.expiry_date < CURRENT_DATE ORDER BY j.id", nativeQuery = true)
	List<Job> findJobsWithExpiredAttachments();

	/**
	 * Atomically sets job status to CLOSED only when no ACTIVE contract exists for the job.
	 *
	 * @return number of rows updated (0 if job not found or an ACTIVE contract exists)
	 */
	@Modifying(clearAutomatically = true)
	@Query(value = """
			UPDATE jobs j
			SET status = 'CLOSED'
			WHERE j.id = :jobId
			  AND NOT EXISTS (
			      SELECT 1 FROM contracts c
			      WHERE c.job_id = j.id AND c.status = 'ACTIVE'
			  )
			""", nativeQuery = true)
	int closeJobIfNoActiveContract(@Param("jobId") Long jobId);

	@Modifying(clearAutomatically = true)
	@Query(value = """
			UPDATE proposals
			SET status = 'REJECTED'
			WHERE job_id = :jobId AND status = 'SUBMITTED'
			""", nativeQuery = true)
	int rejectSubmittedProposalsByJobId(@Param("jobId") Long jobId);
}

