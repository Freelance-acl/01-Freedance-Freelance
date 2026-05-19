package com.team01.freelance.proposal.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public class FeeEstimateRequest {

    @JsonAlias({"bidAmount", "bid_amount"})
    private Double bidAmount;

    @JsonAlias({"estimatedDays", "estimated_days"})
    private Integer estimatedDays;

    public Double getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(Double bidAmount) {
        this.bidAmount = bidAmount;
    }

    public Integer getEstimatedDays() {
        return estimatedDays;
    }

    public void setEstimatedDays(Integer estimatedDays) {
        this.estimatedDays = estimatedDays;
    }
}
