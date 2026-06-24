package com.team01.freelance.wallet.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class PayoutMetrics {

    private final Counter payoutFailures;

    public PayoutMetrics(MeterRegistry registry) {
        this.payoutFailures = Counter.builder("application_payout_failures_total")
                .description("Total number of payout failures")
                .register(registry);
    }

    public void recordPayoutFailure() {
        payoutFailures.increment();
    }
}
