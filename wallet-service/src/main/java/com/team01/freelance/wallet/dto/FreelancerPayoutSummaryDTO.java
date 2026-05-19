package com.team01.freelance.wallet.dto;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Response DTO for [S5-F3] Freelancer Payout Summary.
 *
 * Aggregates COMPLETED payouts for a single freelancer, broken down by
 * payout method (BANK_TRANSFER / PAYPAL / CRYPTO).
 */
public class FreelancerPayoutSummaryDTO {

    private Long freelancerId;
    private Long totalPayouts;
    private Double totalAmount;
    private Map<String, Double> methodBreakdown;

    public FreelancerPayoutSummaryDTO() {
        this.methodBreakdown = new LinkedHashMap<>();
    }

    public FreelancerPayoutSummaryDTO(Long freelancerId,
                                      Long totalPayouts,
                                      Double totalAmount,
                                      Map<String, Double> methodBreakdown) {
        this.freelancerId = freelancerId;
        this.totalPayouts = totalPayouts;
        this.totalAmount = totalAmount;
        this.methodBreakdown = methodBreakdown != null ? methodBreakdown : new LinkedHashMap<>();
    }

    public Long getFreelancerId() {
        return freelancerId;
    }

    public void setFreelancerId(Long freelancerId) {
        this.freelancerId = freelancerId;
    }

    public Long getTotalPayouts() {
        return totalPayouts;
    }

    public void setTotalPayouts(Long totalPayouts) {
        this.totalPayouts = totalPayouts;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Map<String, Double> getMethodBreakdown() {
        return methodBreakdown;
    }

    public void setMethodBreakdown(Map<String, Double> methodBreakdown) {
        this.methodBreakdown = methodBreakdown;
    }
}
