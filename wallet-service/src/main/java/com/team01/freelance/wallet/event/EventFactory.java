package com.team01.freelance.wallet.event;

import com.team01.freelance.common.event.EventType;
import com.team01.freelance.common.event.MongoEvent;
import com.team01.freelance.wallet.model.PayoutMethod;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class EventFactory {

    public MongoEvent createEvent(EventType type, Map<String, Object> params) {
        if (type != EventType.PAYOUT_AUDIT) {
            throw new IllegalArgumentException("wallet EventFactory only supports PAYOUT_AUDIT, got " + type);
        }
        PayoutAuditEvent event = new PayoutAuditEvent();
        event.setPayoutId(requireLong(params, "payoutId"));
        event.setAction(requireString(params, "action"));
        event.setTimestamp(params.containsKey("timestamp")
                ? (LocalDateTime) params.get("timestamp")
                : LocalDateTime.now());
        if (params.containsKey("method")) {
            Object method = params.get("method");
            if (method instanceof PayoutMethod payoutMethod) {
                event.setMethod(payoutMethod);
            } else {
                event.setMethod(PayoutMethod.valueOf(method.toString()));
            }
        }
        if (params.containsKey("amount")) {
            event.setAmount(((Number) params.get("amount")).doubleValue());
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> details = params.containsKey("details")
                ? (Map<String, Object>) params.get("details")
                : Map.of();
        event.setDetails(details);
        return event;
    }

    private static Long requireLong(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalArgumentException("Missing or invalid parameter: " + key);
    }

    private static String requireString(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing parameter: " + key);
        }
        return value.toString();
    }
}
