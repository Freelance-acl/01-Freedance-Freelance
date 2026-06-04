package com.team01.freelance.proposal.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.team01.freelance.proposal.event.ProposalEvent;

@Repository
public interface ProposalEventRepository extends MongoRepository<ProposalEvent, String> {
}
