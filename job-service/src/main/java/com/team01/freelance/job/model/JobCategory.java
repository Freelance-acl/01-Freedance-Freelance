package com.team01.freelance.job.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum JobCategory {
    WEB_DEV,
    MOBILE,
    DESIGN,
    WRITING;

    @JsonCreator
    public static JobCategory fromString(String value) {
        for (JobCategory category : JobCategory.values()) {
            if (category.name().equalsIgnoreCase(value)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Invalid JobCategory: " + value + ". Valid options are: WEB_DEV, MOBILE, DESIGN, WRITING");
    }
}

