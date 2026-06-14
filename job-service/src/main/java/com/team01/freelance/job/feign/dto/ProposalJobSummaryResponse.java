package com.team01.freelance.job.feign.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProposalJobSummaryResponse {

    private Long totalProposals;
    private Long acceptedProposals;
    private Double averageBidAmount;
    private Double lowestBid;
    private Double highestBid;

    public Long getTotalProposals() {
        return totalProposals;
    }

    public void setTotalProposals(Long totalProposals) {
        this.totalProposals = totalProposals;
    }

    public Long getAcceptedProposals() {
        return acceptedProposals;
    }

    public void setAcceptedProposals(Long acceptedProposals) {
        this.acceptedProposals = acceptedProposals;
    }

    public Double getAverageBidAmount() {
        return averageBidAmount;
    }

    public void setAverageBidAmount(Double averageBidAmount) {
        this.averageBidAmount = averageBidAmount;
    }

    public Double getLowestBid() {
        return lowestBid;
    }

    public void setLowestBid(Double lowestBid) {
        this.lowestBid = lowestBid;
    }

    public Double getHighestBid() {
        return highestBid;
    }

    public void setHighestBid(Double highestBid) {
        this.highestBid = highestBid;
    }
}
