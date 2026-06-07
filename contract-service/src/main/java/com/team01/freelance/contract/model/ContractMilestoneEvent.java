package com.team01.freelance.contract.model;

import com.team01.freelance.proposal.model.MilestoneStatus;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

@Table("contract_milestone_events")
public class ContractMilestoneEvent {

    @PrimaryKey
    private ContractMilestoneEventKey key;
    private Integer milestoneOrder;
    private MilestoneStatus status;
    private String recordedBy;
    private String notes;

    public ContractMilestoneEventKey getKey() {
        return key;
    }

    public void setKey(ContractMilestoneEventKey key) {
        this.key = key;
    }

    public Integer getMilestoneOrder() {
        return milestoneOrder;
    }

    public void setMilestoneOrder(Integer milestoneOrder) {
        this.milestoneOrder = milestoneOrder;
    }

    public MilestoneStatus getStatus() {
        return status;
    }

    public void setStatus(MilestoneStatus status) {
        this.status = status;
    }

    public String getRecordedBy() {
        return recordedBy;
    }

    public void setRecordedBy(String recordedBy) {
        this.recordedBy = recordedBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
