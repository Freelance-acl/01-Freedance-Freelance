package com.team01.freelance.contracts.events;

public record JobClosedEvent(
        Long jobId,
        Long clientId
) {
    public static final String ROUTING_KEY = "job.closed";
}
