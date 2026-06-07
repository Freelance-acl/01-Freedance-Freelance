package com.team01.freelance.job.dto;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class JobEventDTO {

    private String action;
    private Long jobId;
    private LocalDateTime timestamp;
    private Map<String, Object> details = new HashMap<>();

    public JobEventDTO() {
    }

    public JobEventDTO(String action, Long jobId, LocalDateTime timestamp, Map<String, Object> details) {
        this.action = action;
        this.jobId = jobId;
        this.timestamp = timestamp;
        this.details = details != null ? new HashMap<>(details) : new HashMap<>();
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
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
