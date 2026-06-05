package com.team01.freelance.job.event;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class EventFactory {

    @SuppressWarnings("unchecked")
    public JobEvent createJobEvent(String eventType, Object payload) {
        Map<String, Object> params = payload instanceof Map<?, ?>
                ? (Map<String, Object>) payload
                : Map.of();
        Long jobId = toLong(params.get("jobId"));
        LocalDateTime timestamp = params.containsKey("timestamp")
                ? requireTimestamp(params, "timestamp")
                : LocalDateTime.now();
        Map<String, Object> details = params.containsKey("details")
                ? requireMap(params, "details")
                : Map.of();
        String action = params.get("action") != null ? String.valueOf(params.get("action")) : eventType;
        return new JobEvent(jobId, action, timestamp, details);
    }

    private LocalDateTime requireTimestamp(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value instanceof LocalDateTime timestamp) {
            return timestamp;
        }
        throw new IllegalArgumentException(key + " must be a LocalDateTime when present");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> requireMap(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value instanceof Map<?, ?> mapValue) {
            return (Map<String, Object>) mapValue;
        }
        throw new IllegalArgumentException(key + " must be a map when present");
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }
}