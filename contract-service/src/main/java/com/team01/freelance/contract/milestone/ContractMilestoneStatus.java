package com.team01.freelance.contract.milestone;

public enum ContractMilestoneStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    APPROVED;

    public static ContractMilestoneStatus fromString(String value) {
        for (ContractMilestoneStatus status : values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid milestone status: " + value);
    }
}
