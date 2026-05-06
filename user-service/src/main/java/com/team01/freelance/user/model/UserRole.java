package com.team01.freelance.user.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum UserRole {
    FREELANCER,
    CLIENT,
    ADMIN;

    @JsonCreator
    public static UserRole fromString(String value) {
        for (UserRole role : UserRole.values()) {
            if (role.name().equalsIgnoreCase(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Invalid UserRole: " + value + ". Valid options are: FREELANCER, CLIENT, ADMIN");
    }
}

