package com.team01.freelance.job.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum JobStatus {
    OPEN,
    IN_PROGRESS,
    CLOSED;

    @JsonCreator
    public static JobStatus fromString(String value) {
        for (JobStatus status : JobStatus.values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid JobStatus: " + value + ". Valid options are: OPEN, IN_PROGRESS, CLOSED");
    }
}

