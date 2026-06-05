package com.team01.freelance.job.event;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Document(collection = "job_events")
public class JobEvent {

    @Id
    private String id;

    private String eventType;
    private Long jobId;
    private LocalDateTime timestamp;
    private Map<String, Object> details = new HashMap<>();

    public JobEvent() {
    }

    public JobEvent(String eventType, Long jobId, LocalDateTime timestamp, Map<String, Object> details) {
        this.eventType = eventType;
        this.jobId = jobId;
        this.timestamp = timestamp;
        if (details != null) {
            this.details = new HashMap<>(details);
        }
    }

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

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details != null ? new HashMap<>(details) : new HashMap<>();
    }
}
