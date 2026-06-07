package com.team01.freelance.proposal.dto;

import java.util.Map;

public class ProposalAnalyticsDashboardDTO {

    private Long totalProposals;
    private Double acceptanceRate;
    private Double averageBidAmount;
    private Double averageEstimatedDays;
    private Map<String, Long> proposalsByStatus;

    public ProposalAnalyticsDashboardDTO() {
    }

    private ProposalAnalyticsDashboardDTO(Builder builder) {
        this.totalProposals = builder.totalProposals;
        this.acceptanceRate = builder.acceptanceRate;
        this.averageBidAmount = builder.averageBidAmount;
        this.averageEstimatedDays = builder.averageEstimatedDays;
        this.proposalsByStatus = builder.proposalsByStatus;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long totalProposals;
        private Double acceptanceRate;
        private Double averageBidAmount;
        private Double averageEstimatedDays;
        private Map<String, Long> proposalsByStatus;

        private Builder() {
        }

        public Builder totalProposals(Long totalProposals) {
            this.totalProposals = totalProposals;
            return this;
        }

        public Builder acceptanceRate(Double acceptanceRate) {
            this.acceptanceRate = acceptanceRate;
            return this;
        }

        public Builder averageBidAmount(Double averageBidAmount) {
            this.averageBidAmount = averageBidAmount;
            return this;
        }

        public Builder averageEstimatedDays(Double averageEstimatedDays) {
            this.averageEstimatedDays = averageEstimatedDays;
            return this;
        }

        public Builder proposalsByStatus(Map<String, Long> proposalsByStatus) {
            this.proposalsByStatus = proposalsByStatus;
            return this;
        }

        public ProposalAnalyticsDashboardDTO build() {
            return new ProposalAnalyticsDashboardDTO(this);
        }
    }

    public Long getTotalProposals() { return totalProposals; }
    public void setTotalProposals(Long totalProposals) { this.totalProposals = totalProposals; }

    public Double getAcceptanceRate() { return acceptanceRate; }
    public void setAcceptanceRate(Double acceptanceRate) { this.acceptanceRate = acceptanceRate; }

    public Double getAverageBidAmount() { return averageBidAmount; }
    public void setAverageBidAmount(Double averageBidAmount) { this.averageBidAmount = averageBidAmount; }

    public Double getAverageEstimatedDays() { return averageEstimatedDays; }
    public void setAverageEstimatedDays(Double averageEstimatedDays) { this.averageEstimatedDays = averageEstimatedDays; }

    public Map<String, Long> getProposalsByStatus() { return proposalsByStatus; }
    public void setProposalsByStatus(Map<String, Long> proposalsByStatus) { this.proposalsByStatus = proposalsByStatus; }
}
