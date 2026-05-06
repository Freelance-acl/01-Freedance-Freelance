package com.team01.freelance.proposal.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ProposalStatus {
    SUBMITTED,
    SHORTLISTED,
    ACCEPTED,
    REJECTED,
    WITHDRAWN;

    @JsonCreator
    public static ProposalStatus fromString(String value) {
        for (ProposalStatus status : ProposalStatus.values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid ProposalStatus: " + value + ". Valid options are: SUBMITTED, SHORTLISTED, ACCEPTED, REJECTED, WITHDRAWN");
    }
}

