package com.team01.freelance.contract.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ContractStatus {
    ACTIVE,
    COMPLETED,
    TERMINATED,
    DISPUTED;

    @JsonCreator
    public static ContractStatus fromString(String value) {
        for (ContractStatus status : ContractStatus.values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid ContractStatus: " + value + ". Valid options are: ACTIVE, COMPLETED, TERMINATED, DISPUTED");
    }
}

