package com.team01.freelance.contract.dto;

public class FreelancerPerformanceDTO {
    private Long freelancerId;
    private Long totalContracts;
    private Double averageContractValue;
    private Double completionRate;
    private Double averageDurationDays;
    private Double totalEarnings;

    public FreelancerPerformanceDTO() {
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

    public Long getFreelancerId() {
        return freelancerId;
    }

    public void setFreelancerId(Long freelancerId) {
        this.freelancerId = freelancerId;
    }

    public Long getTotalContracts() {
        return totalContracts;
    }

    public void setTotalContracts(Long totalContracts) {
        this.totalContracts = totalContracts;
    }

    public Double getAverageContractValue() {
        return averageContractValue;
    }

    public void setAverageContractValue(Double averageContractValue) {
        this.averageContractValue = averageContractValue;
    }

    public Double getCompletionRate() {
        return completionRate;
    }

    public void setCompletionRate(Double completionRate) {
        this.completionRate = completionRate;
    }

    public Double getAverageDurationDays() {
        return averageDurationDays;
    }

    public void setAverageDurationDays(Double averageDurationDays) {
        this.averageDurationDays = averageDurationDays;
    }

    public Double getTotalEarnings() {
        return totalEarnings;
    }

    public void setTotalEarnings(Double totalEarnings) {
        this.totalEarnings = totalEarnings;
    }
}
