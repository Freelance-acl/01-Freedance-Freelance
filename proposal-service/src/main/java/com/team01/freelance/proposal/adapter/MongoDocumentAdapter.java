package com.team01.freelance.proposal.adapter;

import org.springframework.stereotype.Component;

import com.team01.freelance.proposal.dto.ProposalEventDTO;
import com.team01.freelance.proposal.event.ProposalEvent;

@Component
public class MongoDocumentAdapter {

    public ProposalEventDTO adapt(ProposalEvent event) {
        return new ProposalEventDTO(
                event.getProposalId(),
                event.getAction(),
                event.getTimestamp(),
                event.getDetails());
    }
}
