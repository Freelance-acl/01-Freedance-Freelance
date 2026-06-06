package com.team01.freelance.contract.audit;

import java.time.LocalDateTime;
import java.util.Map;

public class ContractEvent {
    private final Long contractId;
    private final String action;
    private final LocalDateTime timestamp;
    private final Map<String, Object> details;

    public ContractEvent(Long contractId, String action, LocalDateTime timestamp, Map<String, Object> details) {
        this.contractId = contractId;
        this.action = action;
        this.timestamp = timestamp;
        this.details = details;
    }

    public Long getContractId() { return contractId; }
    public String getAction() { return action; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public Map<String, Object> getDetails() { return details; }
}
