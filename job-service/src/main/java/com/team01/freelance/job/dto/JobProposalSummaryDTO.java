package com.team01.freelance.job.dto;

/**
 * Response DTO for [S2-F3] Get Job Proposal Summary.
 *
 * Aggregates proposal statistics for a job within a date range:
 * totalProposals, averageBidAmount, lowestBid, highestBid.
 */
public class JobProposalSummaryDTO {
    private Long jobId;
    private String title;
    private Long totalProposals;
    private Double averageBidAmount;
    private Double lowestBid;
    private Double highestBid;

    public JobProposalSummaryDTO() {
    }

    public JobProposalSummaryDTO(Long jobId, String title, Long totalProposals,
                                 Double averageBidAmount, Double lowestBid, Double highestBid) {
        this.jobId = jobId;
        this.title = title;
        this.totalProposals = totalProposals;
        this.averageBidAmount = averageBidAmount;
        this.lowestBid = lowestBid;
        this.highestBid = highestBid;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long jobId;
        private String title;
        private Long totalProposals;
        private Double averageBidAmount;
        private Double lowestBid;
        private Double highestBid;

        private Builder() {}

        public Builder jobId(Long jobId) { this.jobId = jobId; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder totalProposals(Long totalProposals) { this.totalProposals = totalProposals; return this; }
        public Builder averageBidAmount(Double averageBidAmount) { this.averageBidAmount = averageBidAmount; return this; }
        public Builder lowestBid(Double lowestBid) { this.lowestBid = lowestBid; return this; }
        public Builder highestBid(Double highestBid) { this.highestBid = highestBid; return this; }

        public JobProposalSummaryDTO build() {
            return new JobProposalSummaryDTO(jobId, title, totalProposals, averageBidAmount, lowestBid, highestBid);
        }
    }

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Long getTotalProposals() { return totalProposals; }
    public void setTotalProposals(Long totalProposals) { this.totalProposals = totalProposals; }
    public Double getAverageBidAmount() { return averageBidAmount; }
    public void setAverageBidAmount(Double averageBidAmount) { this.averageBidAmount = averageBidAmount; }
    public Double getLowestBid() { return lowestBid; }
    public void setLowestBid(Double lowestBid) { this.lowestBid = lowestBid; }
    public Double getHighestBid() { return highestBid; }
    public void setHighestBid(Double highestBid) { this.highestBid = highestBid; }
}
