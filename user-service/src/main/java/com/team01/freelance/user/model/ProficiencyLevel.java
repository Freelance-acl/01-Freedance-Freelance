package com.team01.freelance.user.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ProficiencyLevel {
    BEGINNER,
    INTERMEDIATE,
    EXPERT;

    @JsonCreator
    public static ProficiencyLevel fromString(String value) {
        for (ProficiencyLevel level : ProficiencyLevel.values()) {
            if (level.name().equalsIgnoreCase(value)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Invalid ProficiencyLevel: " + value + ". Valid options are: BEGINNER, INTERMEDIATE, EXPERT");
    }
}

