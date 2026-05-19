package com.team01.freelance.proposal.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public class FeeEstimateDTO {

    @JsonAlias({"bidAmount", "bid_amount"})
    private final double bidAmount;

    @JsonAlias({"platformFee", "platform_fee"})
    private final double platformFee;

    @JsonAlias({"freelancerPayout", "freelancer_payout"})
    private final double freelancerPayout;

    @JsonAlias({"feePercentage", "fee_percentage"})
    private final int feePercentage;

    @JsonAlias({"estimatedDailyRate", "estimated_daily_rate"})
    private final double estimatedDailyRate;

    public FeeEstimateDTO(
            double bidAmount,
            double platformFee,
            double freelancerPayout,
            int feePercentage,
            double estimatedDailyRate) {
        this.bidAmount = bidAmount;
        this.platformFee = platformFee;
        this.freelancerPayout = freelancerPayout;
        this.feePercentage = feePercentage;
        this.estimatedDailyRate = estimatedDailyRate;
    }

    public double getBidAmount() {
        return bidAmount;
    }

    public double getPlatformFee() {
        return platformFee;
    }

    public double getFreelancerPayout() {
        return freelancerPayout;
    }

    public int getFeePercentage() {
        return feePercentage;
    }

    public double getEstimatedDailyRate() {
        return estimatedDailyRate;
    }
}
