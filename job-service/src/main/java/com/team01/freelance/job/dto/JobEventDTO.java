package com.team01.freelance.job.dto;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Serialized representation of a job event.
 */
public class JobEventDTO {

    private String action;
    private Long jobId;
    private LocalDateTime timestamp;
    private Map<String, Object> details = new HashMap<>();

    /**
     * Creates an empty job event DTO.
     */
    public JobEventDTO() {
    }

    /**
     * Creates a job event DTO with the supplied values.
     */
    public JobEventDTO(String action, Long jobId, LocalDateTime timestamp, Map<String, Object> details) {
        this.action = action;
        this.jobId = jobId;
        this.timestamp = timestamp;
        this.details = details != null ? new HashMap<>(details) : new HashMap<>();
    }

    /**
     * Returns the event action.
     */
    public String getAction() {
        return action;
    }

    /**
     * Sets the event action.
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * Returns the job ID associated with the event.
     */
    public Long getJobId() {
        return jobId;
    }

    /**
     * Sets the job ID associated with the event.
     */
    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    /**
     * Returns the event timestamp.
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the event timestamp.
     */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Returns the event detail payload.
     */
    public Map<String, Object> getDetails() {
        return details;
    }

    /**
     * Sets the event detail payload.
     */
    public void setDetails(Map<String, Object> details) {
        this.details = details != null ? new HashMap<>(details) : new HashMap<>();
    }
}