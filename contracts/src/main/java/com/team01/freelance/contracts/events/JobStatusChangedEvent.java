package com.team01.freelance.contracts.events;

public record JobStatusChangedEvent(
        Long jobId,
        String oldStatus,
        String newStatus
) {
    public static final String ROUTING_KEY = "job.status-changed";
}
