package com.team01.freelance.contract.dto;

public class StalledContractDTO {
    private final Long contractId;
    private final String freelancerName;
    private final String jobTitle;
    private final Double agreedAmount;
    private final Double progressPercentage;
    private final Long daysSinceLastActivity;

    public StalledContractDTO() {
        this.contractId = null;
        this.freelancerName = null;
        this.jobTitle = null;
        this.agreedAmount = null;
        this.progressPercentage = null;
        this.daysSinceLastActivity = null;
    }

    private StalledContractDTO(Builder builder) {
        this.contractId = builder.contractId;
        this.freelancerName = builder.freelancerName;
        this.jobTitle = builder.jobTitle;
        this.agreedAmount = builder.agreedAmount;
        this.progressPercentage = builder.progressPercentage;
        this.daysSinceLastActivity = builder.daysSinceLastActivity;
    }

    public StalledContractDTO(
            Long contractId,
            String freelancerName,
            String jobTitle,
            Double agreedAmount,
            Double progressPercentage,
            Long daysSinceLastActivity
    ) {
        this.contractId = contractId;
        this.freelancerName = freelancerName;
        this.jobTitle = jobTitle;
        this.agreedAmount = agreedAmount;
        this.progressPercentage = progressPercentage;
        this.daysSinceLastActivity = daysSinceLastActivity;
    }

    public static Builder builder() { return new Builder(); }

    public Long getContractId() {
        return contractId;
    }

    public String getFreelancerName() {
        return freelancerName;
    }

    public String getJobTitle() {
        return jobTitle;
    }


    public Double getAgreedAmount() {
        return agreedAmount;
    }


    public Double getProgressPercentage() {
        return progressPercentage;
    }


    public Long getDaysSinceLastActivity() {
        return daysSinceLastActivity;
    }

    public static class Builder {
        private Long contractId;
        private String freelancerName;
        private String jobTitle;
        private Double agreedAmount;
        private Double progressPercentage;
        private Long daysSinceLastActivity;

        public Builder contractId(Long contractId) { this.contractId = contractId; return this; }
        public Builder freelancerName(String freelancerName) { this.freelancerName = freelancerName; return this; }
        public Builder jobTitle(String jobTitle) { this.jobTitle = jobTitle; return this; }
        public Builder agreedAmount(Double agreedAmount) { this.agreedAmount = agreedAmount; return this; }
        public Builder progressPercentage(Double progressPercentage) { this.progressPercentage = progressPercentage; return this; }
        public Builder daysSinceLastActivity(Long daysSinceLastActivity) { this.daysSinceLastActivity = daysSinceLastActivity; return this; }
        public StalledContractDTO build() { return new StalledContractDTO(this); }
    }
}
