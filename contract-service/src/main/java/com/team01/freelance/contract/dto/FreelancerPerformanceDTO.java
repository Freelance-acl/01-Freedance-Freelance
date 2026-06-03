package com.team01.freelance.contract.dto;

public class FreelancerPerformanceDTO {
    private final Long freelancerId;
    private final Long totalContracts;
    private final Double averageContractValue;
    private final Double completionRate;
    private final Double averageDurationDays;
    private final Double totalEarnings;

    public FreelancerPerformanceDTO() {
        this.freelancerId = null;
        this.totalContracts = null;
        this.averageContractValue = null;
        this.completionRate = null;
        this.averageDurationDays = null;
        this.totalEarnings = null;
    }

    private FreelancerPerformanceDTO(Builder builder) {
        this.freelancerId = builder.freelancerId;
        this.totalContracts = builder.totalContracts;
        this.averageContractValue = builder.averageContractValue;
        this.completionRate = builder.completionRate;
        this.averageDurationDays = builder.averageDurationDays;
        this.totalEarnings = builder.totalEarnings;
    }

    public FreelancerPerformanceDTO(
            Long freelancerId,
            Long totalContracts,
            Double averageContractValue,
            Double completionRate,
            Double averageDurationDays,
            Double totalEarnings
    ) {
        this.freelancerId = freelancerId;
        this.totalContracts = totalContracts;
        this.averageContractValue = averageContractValue;
        this.completionRate = completionRate;
        this.averageDurationDays = averageDurationDays;
        this.totalEarnings = totalEarnings;
    }

    public static Builder builder() { return new Builder(); }

    public Long getFreelancerId() {
        return freelancerId;
    }

    public Long getTotalContracts() {
        return totalContracts;
    }

    public Double getAverageContractValue() {
        return averageContractValue;
    }


    public Double getCompletionRate() {
        return completionRate;
    }


    public Double getAverageDurationDays() {
        return averageDurationDays;
    }


    public Double getTotalEarnings() {
        return totalEarnings;
    }

    public static class Builder {
        private Long freelancerId;
        private Long totalContracts;
        private Double averageContractValue;
        private Double completionRate;
        private Double averageDurationDays;
        private Double totalEarnings;

        public Builder freelancerId(Long freelancerId) { this.freelancerId = freelancerId; return this; }
        public Builder totalContracts(Long totalContracts) { this.totalContracts = totalContracts; return this; }
        public Builder averageContractValue(Double averageContractValue) { this.averageContractValue = averageContractValue; return this; }
        public Builder completionRate(Double completionRate) { this.completionRate = completionRate; return this; }
        public Builder averageDurationDays(Double averageDurationDays) { this.averageDurationDays = averageDurationDays; return this; }
        public Builder totalEarnings(Double totalEarnings) { this.totalEarnings = totalEarnings; return this; }
        public FreelancerPerformanceDTO build() { return new FreelancerPerformanceDTO(this); }
    }
}
