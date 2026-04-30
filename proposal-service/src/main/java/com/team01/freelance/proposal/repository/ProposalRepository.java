package com.team01.freelance.proposal.repository;

import com.team01.freelance.proposal.model.Proposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProposalRepository extends JpaRepository<Proposal, Long> {
}

