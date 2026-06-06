package com.team01.freelance.job.event;

import com.team01.freelance.common.event.EventType;
import com.team01.freelance.common.event.MongoEvent;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class EventFactory {

    public MongoEvent createEvent(EventType type, Map<String, Object> params) {
        if (type != EventType.JOB) {
            throw new IllegalArgumentException("job-service EventFactory only supports JOB, got " + type);
        }
        Long jobId = requireLong(params, "jobId");
        String action = requireString(params, "action");
        LocalDateTime timestamp = params.containsKey("timestamp")
                ? requireTimestamp(params, "timestamp")
                : LocalDateTime.now();
        Map<String, Object> details = params.containsKey("details")
                ? requireMap(params, "details")
                : Map.of();
        return new JobEvent(jobId, action, timestamp, details);
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

    private static LocalDateTime requireTimestamp(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value instanceof LocalDateTime timestamp) {
            return timestamp;
        }
        throw new IllegalArgumentException("Missing or invalid parameter: " + key);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> requireMap(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        throw new IllegalArgumentException("Missing or invalid parameter: " + key);
    }
}