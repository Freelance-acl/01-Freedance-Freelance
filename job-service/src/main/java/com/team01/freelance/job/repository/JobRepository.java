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

	@Modifying
	@Query(value = "UPDATE jobs SET status = 'OPEN' WHERE id = :jobId AND status = 'IN_PROGRESS'", nativeQuery = true)
	int reopenIfInProgress(@Param("jobId") Long jobId);
}
