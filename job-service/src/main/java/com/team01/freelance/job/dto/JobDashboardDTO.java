package com.team01.freelance.job.dto;

public class JobDashboardDTO {

    private Long jobId;
    private String title;
    private Long totalProposals;
    private Long acceptedProposals;
    private Double averageBidAmount;
    private Long activeAttachments;
    private Double rating;

    public JobDashboardDTO() {
    }

    public JobDashboardDTO(Long jobId, String title, Long totalProposals, Long acceptedProposals,
                           Double averageBidAmount, Long activeAttachments, Double rating) {
        this.jobId = jobId;
        this.title = title;
        this.totalProposals = totalProposals;
        this.acceptedProposals = acceptedProposals;
        this.averageBidAmount = averageBidAmount;
        this.activeAttachments = activeAttachments;
        this.rating = rating;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long jobId;
        private String title;
        private Long totalProposals;
        private Long acceptedProposals;
        private Double averageBidAmount;
        private Long activeAttachments;
        private Double rating;

        private Builder() {
        }

        public Builder jobId(Long jobId) {
            this.jobId = jobId;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder totalProposals(Long totalProposals) {
            this.totalProposals = totalProposals;
            return this;
        }

        public Builder acceptedProposals(Long acceptedProposals) {
            this.acceptedProposals = acceptedProposals;
            return this;
        }

        public Builder averageBidAmount(Double averageBidAmount) {
            this.averageBidAmount = averageBidAmount;
            return this;
        }

        public Builder activeAttachments(Long activeAttachments) {
            this.activeAttachments = activeAttachments;
            return this;
        }

        public Builder rating(Double rating) {
            this.rating = rating;
            return this;
        }

        public JobDashboardDTO build() {
            return new JobDashboardDTO(jobId, title, totalProposals, acceptedProposals,
                    averageBidAmount, activeAttachments, rating);
        }
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public Long getActiveAttachments() {
        return activeAttachments;
    }

    public void setActiveAttachments(Long activeAttachments) {
        this.activeAttachments = activeAttachments;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }
}