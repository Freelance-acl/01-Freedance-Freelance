package com.team01.freelance.job.dto;

/**
 * Dashboard aggregate view for a job.
 */
public class JobDashboardDTO {

    private Long jobId;
    private String title;
    private Long totalProposals;
    private Long acceptedProposals;
    private Double averageBidAmount;
    private Long activeAttachments;
    private Double rating;

    /**
     * Creates an empty dashboard DTO.
     */
    public JobDashboardDTO() {
    }

    /**
     * Creates a dashboard DTO with all aggregate fields.
     */
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

    /**
     * Returns a builder for the DTO.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for JobDashboardDTO.
     */
    public static final class Builder {
        private Long jobId;
        private String title;
        private Long totalProposals;
        private Long acceptedProposals;
        private Double averageBidAmount;
        private Long activeAttachments;
        private Double rating;

        /**
         * Creates a new builder instance.
         */
        private Builder() {
        }

        /**
         * Sets the job ID.
         */
        public Builder jobId(Long jobId) {
            this.jobId = jobId;
            return this;
        }

        /**
         * Sets the job title.
         */
        public Builder title(String title) {
            this.title = title;
            return this;
        }

        /**
         * Sets the total proposal count.
         */
        public Builder totalProposals(Long totalProposals) {
            this.totalProposals = totalProposals;
            return this;
        }

        /**
         * Sets the accepted proposal count.
         */
        public Builder acceptedProposals(Long acceptedProposals) {
            this.acceptedProposals = acceptedProposals;
            return this;
        }

        /**
         * Sets the average bid amount.
         */
        public Builder averageBidAmount(Double averageBidAmount) {
            this.averageBidAmount = averageBidAmount;
            return this;
        }

        /**
         * Sets the active attachment count.
         */
        public Builder activeAttachments(Long activeAttachments) {
            this.activeAttachments = activeAttachments;
            return this;
        }

        /**
         * Sets the rating.
         */
        public Builder rating(Double rating) {
            this.rating = rating;
            return this;
        }

        /**
         * Builds the DTO instance.
         */
        public JobDashboardDTO build() {
            return new JobDashboardDTO(jobId, title, totalProposals, acceptedProposals,
                    averageBidAmount, activeAttachments, rating);
        }
    }

    /**
     * Returns the job ID.
     */
    public Long getJobId() {
        return jobId;
    }

    /**
     * Sets the job ID.
     */
    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    /**
     * Returns the job title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the job title.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Returns the total proposal count.
     */
    public Long getTotalProposals() {
        return totalProposals;
    }

    /**
     * Sets the total proposal count.
     */
    public void setTotalProposals(Long totalProposals) {
        this.totalProposals = totalProposals;
    }

    /**
     * Returns the accepted proposal count.
     */
    public Long getAcceptedProposals() {
        return acceptedProposals;
    }

    /**
     * Sets the accepted proposal count.
     */
    public void setAcceptedProposals(Long acceptedProposals) {
        this.acceptedProposals = acceptedProposals;
    }

    /**
     * Returns the average bid amount.
     */
    public Double getAverageBidAmount() {
        return averageBidAmount;
    }

    /**
     * Sets the average bid amount.
     */
    public void setAverageBidAmount(Double averageBidAmount) {
        this.averageBidAmount = averageBidAmount;
    }

    /**
     * Returns the active attachment count.
     */
    public Long getActiveAttachments() {
        return activeAttachments;
    }

    /**
     * Sets the active attachment count.
     */
    public void setActiveAttachments(Long activeAttachments) {
        this.activeAttachments = activeAttachments;
    }

    /**
     * Returns the rating.
     */
    public Double getRating() {
        return rating;
    }

    /**
     * Sets the rating.
     */
    public void setRating(Double rating) {
        this.rating = rating;
    }
}