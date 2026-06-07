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

    public static Builder builder() {
        return new Builder();
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

    public static final class Builder {
        private final FreelancerPerformanceDTO dto = new FreelancerPerformanceDTO();

        public Builder freelancerId(Long freelancerId) {
            dto.setFreelancerId(freelancerId);
            return this;
        }

        public Builder totalContracts(Long totalContracts) {
            dto.setTotalContracts(totalContracts);
            return this;
        }

        public Builder averageContractValue(Double averageContractValue) {
            dto.setAverageContractValue(averageContractValue);
            return this;
        }

        public Builder completionRate(Double completionRate) {
            dto.setCompletionRate(completionRate);
            return this;
        }

        public Builder averageDurationDays(Double averageDurationDays) {
            dto.setAverageDurationDays(averageDurationDays);
            return this;
        }

        public Builder totalEarnings(Double totalEarnings) {
            dto.setTotalEarnings(totalEarnings);
            return this;
        }

        public FreelancerPerformanceDTO build() {
            return dto;
        }
    }
}
