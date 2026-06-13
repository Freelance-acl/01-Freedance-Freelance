package com.team01.freelance.proposal.dto;

public class JobProposalSummaryByJobDTO {

    private Long jobId;
    private Long totalProposals;
    private Long acceptedProposals;
    private Double averageBidAmount;
    private Double lowestBid;
    private Double highestBid;

    public JobProposalSummaryByJobDTO() {
    }

    public JobProposalSummaryByJobDTO(Long jobId, Long totalProposals, Long acceptedProposals,
                                      Double averageBidAmount, Double lowestBid, Double highestBid) {
        this.jobId = jobId;
        this.totalProposals = totalProposals;
        this.acceptedProposals = acceptedProposals;
        this.averageBidAmount = averageBidAmount;
        this.lowestBid = lowestBid;
        this.highestBid = highestBid;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

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
