// PENDING, COMPLETED, FAILED, REFUNDED
package com.team01.freelance.wallet.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum PayoutStatus {
    PENDING,
    COMPLETED,
    FAILED,
    REFUNDED;

    @JsonCreator
    public static PayoutStatus fromString(String value) {
        for (PayoutStatus status : PayoutStatus.values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid Payout Status: " + value + ". Valid options are: PENDING, COMPLETED, FAILED, REFUNDED");
    }
}
