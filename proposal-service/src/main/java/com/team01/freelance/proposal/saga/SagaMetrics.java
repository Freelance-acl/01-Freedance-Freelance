package com.team01.freelance.proposal.saga;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class SagaMetrics {

    private final Counter proposalsCompleted;

    public SagaMetrics(MeterRegistry registry) {
        this.proposalsCompleted = Counter.builder("application_proposals_completed_total")
                .description("Total number of proposals that completed the saga (reached PAID)")
                .register(registry);
    }

    public void recordProposalCompleted() {
        proposalsCompleted.increment();
    }
}
