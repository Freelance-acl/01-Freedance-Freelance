package com.team01.freelance.proposal.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.team01.freelance.proposal.event.ProposalEvent;

public interface ProposalEventRepository extends MongoRepository<ProposalEvent, String> {
}
