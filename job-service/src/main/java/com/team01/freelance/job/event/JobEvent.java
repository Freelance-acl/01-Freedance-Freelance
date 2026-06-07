package com.team01.freelance.job.event;

import com.team01.freelance.common.event.MongoEvent;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Document(collection = "job_events")
public class JobEvent implements MongoEvent {

    @Id
    private String id;

    private String action;
    private Long jobId;
    private String action;
    private LocalDateTime timestamp;
    private Map<String, Object> details = new HashMap<>();

    public JobEvent() {
    }

    public JobEvent(Long jobId, String action, LocalDateTime timestamp, Map<String, Object> details) {
        this.jobId = jobId;
        this.action = action;
        this.timestamp = timestamp;
        if (details != null) {
            this.details = new HashMap<>(details);
        }
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    // Backward compatibility - delegates to getAction()
    public String getEventType() {
        return getAction();
    }

    public void setEventType(String eventType) {
        this.action = eventType;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    @Override
    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    @Override
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public Map<String, Object> getDetails() {
        if (details == null) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new HashMap<>(details));
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details != null ? new HashMap<>(details) : new HashMap<>();
    }
}
