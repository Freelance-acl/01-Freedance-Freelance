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

    public static Builder builder() {
        return new Builder();
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

    public static class Builder {
        private double bidAmount;
        private double platformFee;
        private double freelancerPayout;
        private int feePercentage;
        private double estimatedDailyRate;

        public Builder bidAmount(double bidAmount) {
            this.bidAmount = bidAmount;
            return this;
        }

        public Builder platformFee(double platformFee) {
            this.platformFee = platformFee;
            return this;
        }

        public Builder freelancerPayout(double freelancerPayout) {
            this.freelancerPayout = freelancerPayout;
            return this;
        }

        public Builder feePercentage(int feePercentage) {
            this.feePercentage = feePercentage;
            return this;
        }

        public Builder estimatedDailyRate(double estimatedDailyRate) {
            this.estimatedDailyRate = estimatedDailyRate;
            return this;
        }

        public FeeEstimateDTO build() {
            return new FeeEstimateDTO(
                    bidAmount,
                    platformFee,
                    freelancerPayout,
                    feePercentage,
                    estimatedDailyRate);
        }
    }
}
