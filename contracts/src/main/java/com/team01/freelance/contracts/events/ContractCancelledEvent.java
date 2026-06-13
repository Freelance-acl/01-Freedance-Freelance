package com.team01.freelance.contracts.events;

public record ContractCancelledEvent(
        Long contractId,
        Long proposalId
) {
    public static final String ROUTING_KEY = "contract.cancelled";
}
