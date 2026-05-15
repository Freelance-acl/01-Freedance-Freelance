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

    @Query("""
            SELECT DISTINCT p FROM Proposal p
            LEFT JOIN FETCH p.proposalMilestones
            WHERE p.id = :id
            """)
    Optional<Proposal> findByIdWithMilestones(@Param("id") Long id);
}
