package com.team01.freelance.contracts.events;

public record ContractStatusChangedEvent(
        Long contractId,
        String oldStatus,
        String newStatus
) {
    public static final String ROUTING_KEY = "contract.status-changed";
}
