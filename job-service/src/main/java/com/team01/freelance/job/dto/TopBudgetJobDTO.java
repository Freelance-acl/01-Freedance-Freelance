package com.team01.freelance.job.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * DTO for top budget jobs report.
 * Contains job information for the top jobs ordered by budgetMax descending.
 */
public class TopBudgetJobDTO {

    private Long jobId;

    private String title;

    private Double budgetMax;

    @JsonAlias({"totalProposals", "total_proposals"})
    private Long totalProposals;

    /**
     * Default constructor
     */
    public TopBudgetJobDTO() {
    }

    /**
     * Constructor with all fields
     */
    public TopBudgetJobDTO(Long jobId, String title, Double budgetMax, Long totalProposals) {
        this.jobId = jobId;
        this.title = title;
        this.budgetMax = budgetMax;
        this.totalProposals = totalProposals;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Long jobId;
        private String title;
        private Double budgetMax;
        private Long totalProposals;

        public Builder jobId(Long jobId) {
            this.jobId = jobId;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder budgetMax(Double budgetMax) {
            this.budgetMax = budgetMax;
            return this;
        }

        public Builder totalProposals(Long totalProposals) {
            this.totalProposals = totalProposals;
            return this;
        }

        public TopBudgetJobDTO build() {
            return new TopBudgetJobDTO(jobId, title, budgetMax, totalProposals);
        }
    }

    // Getters and Setters

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

    public Double getBudgetMax() {
        return budgetMax;
    }

    public void setBudgetMax(Double budgetMax) {
        this.budgetMax = budgetMax;
    }

    public Long getTotalProposals() {
        return totalProposals;
    }

    public void setTotalProposals(Long totalProposals) {
        this.totalProposals = totalProposals;
    }

    @Override
    public String toString() {
        return "TopBudgetJobDTO{" +
                "jobId=" + jobId +
                ", title='" + title + '\'' +
                ", budgetMax=" + budgetMax +
                ", totalProposals=" + totalProposals +
                '}';
    }
}
