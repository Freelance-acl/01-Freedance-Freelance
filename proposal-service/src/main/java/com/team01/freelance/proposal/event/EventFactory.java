package com.team01.freelance.proposal.event;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.team01.freelance.common.event.EventType;
import com.team01.freelance.common.event.MongoEvent;

@Component
public class EventFactory {

    public MongoEvent createEvent(EventType type, Map<String, Object> params) {
        if (type != EventType.PROPOSAL) {
            throw new IllegalArgumentException("proposal-service EventFactory only supports PROPOSAL, got " + type);
        }
        Long proposalId = requireLong(params, "proposalId");
        String action = requireString(params, "action");
        LocalDateTime timestamp = params.containsKey("timestamp")
                ? (LocalDateTime) params.get("timestamp")
                : LocalDateTime.now();
        @SuppressWarnings("unchecked")
        Map<String, Object> details = params.containsKey("details")
                ? (Map<String, Object>) params.get("details")
                : Map.of();
        return new ProposalEvent(proposalId, action, timestamp, details);
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
