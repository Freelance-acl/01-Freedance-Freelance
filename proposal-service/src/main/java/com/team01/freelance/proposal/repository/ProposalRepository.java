package com.team01.freelance.proposal.repository;

import com.team01.freelance.proposal.model.Proposal;
import com.team01.freelance.proposal.model.ProposalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProposalRepository extends JpaRepository<Proposal, Long> {
    @Query(value = """
            SELECT
                COUNT(*) AS "totalProposals",
                COALESCE(SUM(CASE WHEN status = 'ACCEPTED' THEN 1 ELSE 0 END), 0) AS "acceptedProposals",
                COALESCE(SUM(CASE WHEN status = 'REJECTED' THEN 1 ELSE 0 END), 0) AS "rejectedProposals",
                COALESCE(SUM(bid_amount), 0.0) AS "totalBidValue",
                COALESCE(AVG(bid_amount), 0.0) AS "averageBid",
                CASE
                    WHEN COUNT(*) = 0 THEN 0.0
                    ELSE COALESCE(SUM(CASE WHEN status = 'ACCEPTED' THEN 1 ELSE 0 END), 0) * 100.0 / COUNT(*)
                END AS "acceptanceRate"
            FROM proposals
            WHERE submitted_at >= :start
              AND submitted_at < :endExclusive
            """, nativeQuery = true)
    ProposalAnalyticsProjection calculateAnalyticsBySubmittedAtRange(
            @Param("start") LocalDateTime start,
            @Param("endExclusive") LocalDateTime endExclusive);

    @Query("""
            SELECT p FROM Proposal p
            WHERE p.submittedAt >= :start
              AND p.submittedAt < :endExclusive
              AND (:status IS NULL OR p.status = :status)
            ORDER BY p.submittedAt DESC
            """)
    List<Proposal> searchBySubmittedAtRangeAndOptionalStatus(
            @Param("start") LocalDateTime start,
            @Param("endExclusive") LocalDateTime endExclusive,
            @Param("status") ProposalStatus status);

    @Query(
            value = "SELECT COUNT(*) FROM proposals "
                    + "WHERE status IN ('SUBMITTED', 'SHORTLISTED') "
                    + "AND bid_amount BETWEEN :minBid AND :maxBid",
            nativeQuery = true)
    long countActiveProposalsInSimilarBidRange(
            @Param("minBid") double minBid,
            @Param("maxBid") double maxBid);

    long countByJobIdAndStatusIn(Long jobId, List<ProposalStatus> statuses);

    @Query("""
            SELECT DISTINCT p FROM Proposal p
            LEFT JOIN FETCH p.proposalMilestones
            WHERE p.id = :id
            """)
    Optional<Proposal> findByIdWithMilestones(@Param("id") Long id);
}
