package com.team01.freelance.wallet.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum PayoutMethod {
    BANK_TRANSFER,
    PAYPAL,
    CRYPTO;

    @JsonCreator
    public static PayoutMethod fromString(String value) {
        for (PayoutMethod method : PayoutMethod.values()) {
            if (method.name().equalsIgnoreCase(value)) {
                return method;
            }
        }
        throw new IllegalArgumentException("Invalid Payout Method: " + value + ". Valid options are: BANK_TRANSFER, PAYPAL, STRIPE");
    }
}

