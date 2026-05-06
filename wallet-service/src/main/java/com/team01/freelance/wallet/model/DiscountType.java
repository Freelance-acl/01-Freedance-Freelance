package com.team01.freelance.wallet.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum DiscountType {
    PERCENTAGE,
    FIXED;

    @JsonCreator
    public static DiscountType fromString(String value) {
        for (DiscountType type : DiscountType.values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid DiscountType: " + value + ". Valid options are: PERCENTAGE, FIXED");
    }
}

