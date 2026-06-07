package com.team01.freelance.contract.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public class ContractAnalyticsDTO {
    private final Long totalContracts;
    private final Double avgValue;
    private final Double completionRate;
    private final Double avgDurationDays;
    private final Map<String, Long> byStatus;

    private ContractAnalyticsDTO(Builder builder) {
        this.totalContracts = builder.totalContracts;
        this.avgValue = builder.avgValue;
        this.completionRate = builder.completionRate;
        this.avgDurationDays = builder.avgDurationDays;
        this.byStatus = builder.byStatus;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getTotalContracts() {
        return totalContracts;
    }

    public Double getAvgValue() {
        return avgValue;
    }

    public Double getCompletionRate() {
        return completionRate;
    }

    public Double getAvgDurationDays() {
        return avgDurationDays;
    }

    public Map<String, Long> getByStatus() {
        return byStatus;
    }

    public static class Builder {
        private Long totalContracts;
        private Double avgValue;
        private Double completionRate;
        private Double avgDurationDays;
        private Map<String, Long> byStatus = new LinkedHashMap<>();

        public Builder totalContracts(Long totalContracts) {
            this.totalContracts = totalContracts;
            return this;
        }

        public Builder avgValue(Double avgValue) {
            this.avgValue = avgValue;
            return this;
        }

        public Builder completionRate(Double completionRate) {
            this.completionRate = completionRate;
            return this;
        }

        public Builder avgDurationDays(Double avgDurationDays) {
            this.avgDurationDays = avgDurationDays;
            return this;
        }

        public Builder byStatus(Map<String, Long> byStatus) {
            this.byStatus = new LinkedHashMap<>(byStatus);
            return this;
        }

        public ContractAnalyticsDTO build() {
            return new ContractAnalyticsDTO(this);
        }
    }
}
