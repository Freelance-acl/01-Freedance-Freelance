package com.team01.freelance.contract.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public class ContractAnalyticsDTO {
    private Long totalContracts;
    private Double averageContractValue;
    private Double completionRate;
    private Double averageContractDurationDays;
    private Map<String, Long> contractsByStatus;

    public ContractAnalyticsDTO() {
    }

    public static Builder builder() {
        return new Builder();
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

    public Double getAverageContractDurationDays() {
        return averageContractDurationDays;
    }

    public void setAverageContractDurationDays(Double averageContractDurationDays) {
        this.averageContractDurationDays = averageContractDurationDays;
    }

    public Map<String, Long> getContractsByStatus() {
        return contractsByStatus;
    }

    public void setContractsByStatus(Map<String, Long> contractsByStatus) {
        this.contractsByStatus = contractsByStatus;
    }

    public static final class Builder {
        private final ContractAnalyticsDTO dto = new ContractAnalyticsDTO();

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

        public Builder averageContractDurationDays(Double averageContractDurationDays) {
            dto.setAverageContractDurationDays(averageContractDurationDays);
            return this;
        }

        public Builder contractsByStatus(Map<String, Long> contractsByStatus) {
            dto.setContractsByStatus(new LinkedHashMap<>(contractsByStatus));
            return this;
        }

        public ContractAnalyticsDTO build() {
            if (dto.getContractsByStatus() == null) {
                dto.setContractsByStatus(new LinkedHashMap<>());
            }
            return dto;
        }
    }
}
