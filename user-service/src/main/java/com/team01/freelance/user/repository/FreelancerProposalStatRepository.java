package com.team01.freelance.user.repository;

import com.team01.freelance.user.model.FreelancerProposalStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FreelancerProposalStatRepository extends JpaRepository<FreelancerProposalStat, Long> {
}
