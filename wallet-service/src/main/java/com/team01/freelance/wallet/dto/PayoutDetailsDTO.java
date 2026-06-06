package com.team01.freelance.wallet.dto;

import com.team01.freelance.wallet.model.PayoutMethod;
import com.team01.freelance.wallet.model.PayoutStatus;
import java.util.List;
import java.util.Map;

public record PayoutDetailsDTO(
        Long payoutId,
        Long contractId,
        Long freelancerId,
        Double originalAmount,
        PayoutMethod method,
        PayoutStatus status,
        Map<String, Object> transactionDetails,
        List<AppliedPromoCodeDTO> appliedPromoCodes,
        Double totalDiscount,
        Double finalAmount
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long payoutId;
        private Long contractId;
        private Long freelancerId;
        private Double originalAmount;
        private PayoutMethod method;
        private PayoutStatus status;
        private Map<String, Object> transactionDetails;
        private List<AppliedPromoCodeDTO> appliedPromoCodes;
        private Double totalDiscount;
        private Double finalAmount;

        public Builder payoutId(Long payoutId) {
            this.payoutId = payoutId;
            return this;
        }

        public Builder contractId(Long contractId) {
            this.contractId = contractId;
            return this;
        }

        public Builder freelancerId(Long freelancerId) {
            this.freelancerId = freelancerId;
            return this;
        }

        public Builder originalAmount(Double originalAmount) {
            this.originalAmount = originalAmount;
            return this;
        }

        public Builder method(PayoutMethod method) {
            this.method = method;
            return this;
        }

        public Builder status(PayoutStatus status) {
            this.status = status;
            return this;
        }

        public Builder transactionDetails(Map<String, Object> transactionDetails) {
            this.transactionDetails = transactionDetails;
            return this;
        }

        public Builder appliedPromoCodes(List<AppliedPromoCodeDTO> appliedPromoCodes) {
            this.appliedPromoCodes = appliedPromoCodes;
            return this;
        }

        public Builder totalDiscount(Double totalDiscount) {
            this.totalDiscount = totalDiscount;
            return this;
        }

        public Builder finalAmount(Double finalAmount) {
            this.finalAmount = finalAmount;
            return this;
        }

        public PayoutDetailsDTO build() {
            return new PayoutDetailsDTO(
                    payoutId, contractId, freelancerId, originalAmount, method,
                    status, transactionDetails, appliedPromoCodes, totalDiscount, finalAmount
            );
        }
    }
}