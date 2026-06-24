package com.team01.freelance.wallet.support;

import com.team01.freelance.wallet.observer.EntityObserver;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Test observer — records payout audit payloads and cache-invalidation signals (§10.5.3).
 */
@Component
@Profile("test")
public class TestPayoutEventObserver implements EntityObserver {

    private static final Set<String> CACHE_MUTATING_ACTIONS = Set.of(
            "CREATED", "COMPLETED", "FAILED", "REFUNDED", "REFUND_DENIED", "PROMO_APPLIED");

    private final List<Map<String, Object>> auditEvents = new CopyOnWriteArrayList<>();
    private volatile boolean walletAnalyticsInvalidated;

    @Override
    public void onEvent(String eventType, Object payload) {
        if (!(payload instanceof Map<?, ?> raw)) {
            return;
        }
        Map<String, Object> copy = new HashMap<>();
        raw.forEach((k, v) -> copy.put(String.valueOf(k), v));
        auditEvents.add(copy);

        String action = String.valueOf(copy.get("action"));
        if (CACHE_MUTATING_ACTIONS.contains(action)) {
            walletAnalyticsInvalidated = true;
        }
    }

    public List<Map<String, Object>> getAuditEvents() {
        return List.copyOf(auditEvents);
    }

    public boolean isWalletAnalyticsInvalidated() {
        return walletAnalyticsInvalidated;
    }

    public void reset() {
        auditEvents.clear();
        walletAnalyticsInvalidated = false;
    }

    public boolean hasAuditAction(Long payoutId, String action) {
        return auditEvents.stream().anyMatch(event ->
                action.equals(String.valueOf(event.get("action")))
                        && payoutId.equals(((Number) event.get("payoutId")).longValue()));
    }
}
