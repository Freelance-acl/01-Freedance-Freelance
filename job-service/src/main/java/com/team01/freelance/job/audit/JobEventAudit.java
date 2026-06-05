package com.team01.freelance.job.audit;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * MongoDB document for auditing job events.
 * Stores event type, payload, and timestamp for every significant job operation.
 */
@Document(collection = "job_events")
public class JobEventAudit {

    @Id
    private String id;

    private String eventType;

    private Map<String, Object> payload;

    private LocalDateTime timestamp;

    public JobEventAudit() {
    }

    public JobEventAudit(String eventType, Map<String, Object> payload) {
        this.eventType = eventType;
        this.payload = payload;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "JobEventAudit{" +
                "id='" + id + '\'' +
                ", eventType='" + eventType + '\'' +
                ", payload=" + payload +
                ", timestamp=" + timestamp +
                '}';
    }
}
