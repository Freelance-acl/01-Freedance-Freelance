package com.team01.freelance.job.event;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Document(collection = "job_events")
public class JobEvent {

    @Id
    private String id;
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
        this.details = details != null ? new HashMap<>(details) : new HashMap<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getDetails() {
        return details == null ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(details));
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details != null ? new HashMap<>(details) : new HashMap<>();
    }
}