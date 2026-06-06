package com.team01.freelance.contract.dto;

import java.time.LocalDateTime;

public class ContractMilestoneDTO {
    private final LocalDateTime timestamp;
    private final Integer milestoneOrder;
    private final String status;
    private final String recordedBy;
    private final String notes;

    private ContractMilestoneDTO(Builder builder) {
        this.timestamp = builder.timestamp;
        this.milestoneOrder = builder.milestoneOrder;
        this.status = builder.status;
        this.recordedBy = builder.recordedBy;
        this.notes = builder.notes;
    }

    public static Builder builder() { return new Builder(); }

    public LocalDateTime getTimestamp() { return timestamp; }
    public Integer getMilestoneOrder() { return milestoneOrder; }
    public String getStatus() { return status; }
    public String getRecordedBy() { return recordedBy; }
    public String getNotes() { return notes; }

    public static class Builder {
        private LocalDateTime timestamp;
        private Integer milestoneOrder;
        private String status;
        private String recordedBy;
        private String notes;

        public Builder timestamp(LocalDateTime timestamp) { this.timestamp = timestamp; return this; }
        public Builder milestoneOrder(Integer milestoneOrder) { this.milestoneOrder = milestoneOrder; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder recordedBy(String recordedBy) { this.recordedBy = recordedBy; return this; }
        public Builder notes(String notes) { this.notes = notes; return this; }
        public ContractMilestoneDTO build() { return new ContractMilestoneDTO(this); }
    }
}
