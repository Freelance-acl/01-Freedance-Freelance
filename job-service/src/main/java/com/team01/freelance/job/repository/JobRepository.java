package com.team01.freelance.job.repository;

import com.team01.freelance.job.model.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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


	@Query(value = "SELECT DISTINCT j.* FROM jobs j JOIN job_attachments ja ON ja.job_id = j.id WHERE ja.expiry_date < CURRENT_DATE ORDER BY j.id", nativeQuery = true)
	List<Job> findJobsWithExpiredAttachments();

	@Query(value = "SELECT j.* FROM jobs j " +
			"WHERE j.requirements #>> ARRAY[:key] = :value " +
			"AND (:status IS NULL OR j.status = CAST(:status AS VARCHAR))",
			nativeQuery = true)
	List<Job> searchByRequirements(@Param("key") String key, @Param("value") String value, @Param("status") String status);

	@Query(value = "SELECT j.* FROM jobs j " +
			"WHERE j.requirements #>> ARRAY[:key] = :value",
			nativeQuery = true)
	List<Job> searchByRequirements(@Param("key") String key, @Param("value") String value);

	@Modifying(clearAutomatically = true)
	@Query(value = "UPDATE jobs SET status = 'IN_PROGRESS' WHERE id = :jobId", nativeQuery = true)
	int markJobInProgress(@Param("jobId") Long jobId);

	@Modifying(clearAutomatically = true)
	@Query(value = "UPDATE jobs SET status = 'CLOSED' WHERE id = :jobId", nativeQuery = true)
	int markJobClosed(@Param("jobId") Long jobId);

	Page<Job> findByOrderByBudgetMaxDesc(Pageable pageable);


	@Modifying(clearAutomatically = true)
	@Query(value = "UPDATE jobs SET status = 'OPEN' WHERE id = :jobId AND status = 'IN_PROGRESS'", nativeQuery = true)
	int reopenIfInProgress(@Param("jobId") Long jobId);

	@Query(value = """
			SELECT
			    j.id AS jobId,
			    j.title,
			    j.rating,
			    (SELECT COUNT(*) FROM job_attachments WHERE job_id = j.id AND expiry_date > CURRENT_DATE) AS activeAttachments
			FROM jobs j
			WHERE j.id = :jobId
			""", nativeQuery = true)
	List<Object[]> getJobDashboardBase(@Param("jobId") Long jobId);
}
