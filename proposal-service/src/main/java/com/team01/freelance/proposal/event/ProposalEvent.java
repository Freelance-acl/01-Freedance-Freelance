package com.team01.freelance.proposal.event;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.team01.freelance.common.event.MongoEvent;

@Document(collection = "proposal_events")
public class ProposalEvent implements MongoEvent {

    @Id
    private String id;

    private Long proposalId;
    private String action;
    private LocalDateTime timestamp;
    private Map<String, Object> details = new HashMap<>();

    public ProposalEvent() {
    }

    public ProposalEvent(Long proposalId, String action, LocalDateTime timestamp, Map<String, Object> details) {
        this.proposalId = proposalId;
        this.action = action;
        this.timestamp = timestamp;
        if (details != null) {
            this.details = new HashMap<>(details);
        }
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getProposalId() {
        return proposalId;
    }

    public void setProposalId(Long proposalId) {
        this.proposalId = proposalId;
    }

    @Override
    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    @Override
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details != null ? new HashMap<>(details) : new HashMap<>();
    }
}
