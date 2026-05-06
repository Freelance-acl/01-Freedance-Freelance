package com.team01.freelance.job.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum JobAttachmentType {
    BRIEF,
    MOCKUP,
    REFERENCE,
    CONTRACT_TEMPLATE;

    @JsonCreator
    public static JobAttachmentType fromString(String value) {
        for (JobAttachmentType type : JobAttachmentType.values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid JobAttachmentType: " + value + ". Valid options are: BRIEF, MOCKUP, REFERENCE, CONTRACT_TEMPLATE");
    }
}

