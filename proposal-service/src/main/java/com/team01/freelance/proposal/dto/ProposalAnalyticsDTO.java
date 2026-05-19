package com.team01.freelance.proposal.dto;

public class ProposalAnalyticsDTO {
    private long totalProposals;
    private long acceptedProposals;
    private long rejectedProposals;
    private double totalBidValue;
    private double averageBid;
    private double acceptanceRate;

    public ProposalAnalyticsDTO(
            long totalProposals,
            long acceptedProposals,
            long rejectedProposals,
            double totalBidValue,
            double averageBid,
            double acceptanceRate) {
        this.totalProposals = totalProposals;
        this.acceptedProposals = acceptedProposals;
        this.rejectedProposals = rejectedProposals;
        this.totalBidValue = totalBidValue;
        this.averageBid = averageBid;
        this.acceptanceRate = acceptanceRate;
    }

    public long getTotalProposals() {
        return totalProposals;
    }

    public void setTotalProposals(long totalProposals) {
        this.totalProposals = totalProposals;
    }

    public long getAcceptedProposals() {
        return acceptedProposals;
    }

    public void setAcceptedProposals(long acceptedProposals) {
        this.acceptedProposals = acceptedProposals;
    }

    public long getRejectedProposals() {
        return rejectedProposals;
    }

    public void setRejectedProposals(long rejectedProposals) {
        this.rejectedProposals = rejectedProposals;
    }

    public double getTotalBidValue() {
        return totalBidValue;
    }

    public void setTotalBidValue(double totalBidValue) {
        this.totalBidValue = totalBidValue;
    }

    public double getAverageBid() {
        return averageBid;
    }

    public void setAverageBid(double averageBid) {
        this.averageBid = averageBid;
    }

    public double getAcceptanceRate() {
        return acceptanceRate;
    }

    public void setAcceptanceRate(double acceptanceRate) {
        this.acceptanceRate = acceptanceRate;
    }
}
