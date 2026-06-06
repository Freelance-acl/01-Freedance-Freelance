package com.team01.freelance.wallet.dto;

public record PromoCodeUsageDTO(
        Long promoCodeId,
        String code,
        String discountType,
        Double discountValue,
        Long timesUsed,
        Double totalDiscountGiven,
        Boolean active,
        Boolean expired
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long promoCodeId;
        private String code;
        private String discountType;
        private Double discountValue;
        private Long timesUsed;
        private Double totalDiscountGiven;
        private Boolean active;
        private Boolean expired;

        public Builder promoCodeId(Long promoCodeId) {
            this.promoCodeId = promoCodeId;
            return this;
        }

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder discountType(String discountType) {
            this.discountType = discountType;
            return this;
        }

        public Builder discountValue(Double discountValue) {
            this.discountValue = discountValue;
            return this;
        }

        public Builder timesUsed(Long timesUsed) {
            this.timesUsed = timesUsed;
            return this;
        }

        public Builder totalDiscountGiven(Double totalDiscountGiven) {
            this.totalDiscountGiven = totalDiscountGiven;
            return this;
        }

        public Builder active(Boolean active) {
            this.active = active;
            return this;
        }

        public Builder expired(Boolean expired) {
            this.expired = expired;
            return this;
        }

        public PromoCodeUsageDTO build() {
            return new PromoCodeUsageDTO(
                    promoCodeId, code, discountType, discountValue,
                    timesUsed, totalDiscountGiven, active, expired
            );
        }
    }
}