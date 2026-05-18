package com.team01.freelance.proposal.repository;

import com.team01.freelance.proposal.model.Proposal;
import com.team01.freelance.proposal.model.ProposalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

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

    @Query(value = """
            SELECT p.id
            FROM proposals p
            WHERE jsonb_extract_path_text(p.metadata, :key) = :value
            """, nativeQuery = true)
    List<Long> findIdsByMetadata(@Param("key") String key, @Param("value") String value);
}

