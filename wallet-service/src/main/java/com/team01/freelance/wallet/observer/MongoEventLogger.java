package com.team01.freelance.wallet.observer;

import com.team01.freelance.common.event.EventType;
import com.team01.freelance.common.event.MongoEvent;
import com.team01.freelance.wallet.event.EventFactory;
import com.team01.freelance.wallet.event.PayoutAuditEvent;
import com.team01.freelance.wallet.repository.PayoutAuditEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
@org.springframework.context.annotation.Profile("!test")
public class MongoEventLogger implements EntityObserver {

    private static final Logger log = LoggerFactory.getLogger(MongoEventLogger.class);

    private static final Set<String> ANALYTICS_MUTATING_ACTIONS = Set.of(
            "CREATED", "COMPLETED", "FAILED", "REFUNDED", "REFUND_DENIED", "PROMO_APPLIED");

    private final EventFactory eventFactory;
    private final PayoutAuditEventRepository payoutAuditEventRepository;
    private final WalletCacheInvalidationService cacheInvalidationService;

    public MongoEventLogger(
            EventFactory eventFactory,
            PayoutAuditEventRepository payoutAuditEventRepository,
            WalletCacheInvalidationService cacheInvalidationService) {
        this.eventFactory = eventFactory;
        this.payoutAuditEventRepository = payoutAuditEventRepository;
        this.cacheInvalidationService = cacheInvalidationService;
    }

    @Override
    public void onEvent(String eventType, Object payload) {
        try {
            Map<String, Object> params = toParams(eventType, payload);
            MongoEvent event = eventFactory.createEvent(EventType.PAYOUT_AUDIT, params);
            payoutAuditEventRepository.save((PayoutAuditEvent) event);
            invalidateCaches(params);
        } catch (Exception ex) {
            log.warn("Failed to persist payout audit event {}: {}", eventType, ex.getMessage());
        }
    }

    private void invalidateCaches(Map<String, Object> params) {
        String action = String.valueOf(params.get("action"));
        if (!ANALYTICS_MUTATING_ACTIONS.contains(action)) {
            return;
        }
        cacheInvalidationService.invalidateWalletAnalytics();
        if (params.containsKey("payoutId")) {
            cacheInvalidationService.invalidateWalletPayoutDetail(
                    ((Number) params.get("payoutId")).longValue());
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> toParams(String eventType, Object payload) {
        if (payload instanceof Map<?, ?> map) {
            Map<String, Object> params = new HashMap<>();
            map.forEach((k, v) -> params.put(String.valueOf(k), v));
            if (!params.containsKey("action")) {
                params.put("action", eventType);
            }
            return params;
        }
        throw new IllegalArgumentException("Payout audit payload must be a Map");
    }
}

