package com.team01.freelance.job.repository;

import com.team01.freelance.job.dto.JobProposalSummaryDTO;
import com.team01.freelance.job.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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
    Optional<JobProposalSummaryDTO> getProposalSummary(
            @Param("jobId") Long jobId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDateExclusive") LocalDateTime endDateExclusive
    );
}

