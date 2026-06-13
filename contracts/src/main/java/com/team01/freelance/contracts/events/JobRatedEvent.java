package com.team01.freelance.contracts.events;

public record JobRatedEvent(
        Long jobId,
        Long contractId,
        Double rating,
        Long ratedBy
) {
    public static final String ROUTING_KEY = "job.rated";
}
