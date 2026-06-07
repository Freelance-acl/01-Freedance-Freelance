package com.team01.freelance.contract.milestone;

import com.datastax.oss.driver.api.mapper.annotations.CqlName;
import com.datastax.oss.driver.api.mapper.annotations.Entity;
import com.datastax.oss.driver.api.mapper.annotations.PartitionKey;
import com.datastax.oss.driver.api.mapper.annotations.ClusteringColumn;

import java.time.LocalDateTime;

@Entity
@CqlName("contract_milestone_events")
public class ContractMilestoneEvent {

    @PartitionKey
    @CqlName("contract_id")
    private Long contractId;

    @ClusteringColumn(0)
    @CqlName("timestamp")
    private LocalDateTime timestamp;

    @CqlName("milestone_order")
    private Integer milestoneOrder;

    private ContractMilestoneStatus status;

    @CqlName("recorded_by")
    private String recordedBy;

    private String notes;

    public ContractMilestoneEvent() {
    }

    public ContractMilestoneEvent(
            Long contractId,
            LocalDateTime timestamp,
            Integer milestoneOrder,
            ContractMilestoneStatus status,
            String recordedBy,
            String notes
    ) {
        this.contractId = contractId;
        this.timestamp = timestamp;
        this.milestoneOrder = milestoneOrder;
        this.status = status;
        this.recordedBy = recordedBy;
        this.notes = notes;
    }

    public Long contractId() {
        return contractId;
    }

    public LocalDateTime timestamp() {
        return timestamp;
    }

    public Integer milestoneOrder() {
        return milestoneOrder;
    }

    public ContractMilestoneStatus status() {
        return status;
    }

    public String recordedBy() {
        return recordedBy;
    }

    public String notes() {
        return notes;
    }

    public Long getContractId() {
        return contractId;
    }

    public void setContractId(Long contractId) {
        this.contractId = contractId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getMilestoneOrder() {
        return milestoneOrder;
    }

    public void setMilestoneOrder(Integer milestoneOrder) {
        this.milestoneOrder = milestoneOrder;
    }

    public ContractMilestoneStatus getStatus() {
        return status;
    }

    public void setStatus(ContractMilestoneStatus status) {
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
