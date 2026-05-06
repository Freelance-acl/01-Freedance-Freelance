package com.team01.freelance.proposal.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum MilestoneStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    APPROVED;

    @JsonCreator
    public static MilestoneStatus fromString(String value) {
        for (MilestoneStatus status : MilestoneStatus.values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid MilestoneStatus: " + value + ". Valid options are: PENDING, IN_PROGRESS, COMPLETED, APPROVED");
    }
}

