package com.team01.freelance.user.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum UserStatus {
    ACTIVE,
    DEACTIVATED;

    @JsonCreator
    public static UserStatus fromString(String value) {
        for (UserStatus status : UserStatus.values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid UserStatus: " + value + ". Valid options are: ACTIVE, DEACTIVATED");
    }
}

