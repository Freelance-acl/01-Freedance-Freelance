package com.team01.freelance.user.dto;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class UserActivityEventDTO {
    private String id;
    private Long userId;
    private String action;
    private LocalDateTime timestamp;
    private Map<String, Object> details = new HashMap<>();

    public UserActivityEventDTO() {
    }

    public UserActivityEventDTO(String id, Long userId, String action,
                                LocalDateTime timestamp, Map<String, Object> details) {
        this.id = id;
        this.userId = userId;
        this.action = action;
        this.timestamp = timestamp;
        this.details = details != null ? new HashMap<>(details) : new HashMap<>();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private Long userId;
        private String action;
        private LocalDateTime timestamp;
        private Map<String, Object> details = new HashMap<>();

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder action(String action) {
            this.action = action;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder details(Map<String, Object> details) {
            this.details = details != null ? new HashMap<>(details) : new HashMap<>();
            return this;
        }

        public UserActivityEventDTO build() {
            return new UserActivityEventDTO(id, userId, action, timestamp, details);
        }
    }

    public String getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getAction() {
        return action;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details != null ? new HashMap<>(details) : new HashMap<>();
    }
}