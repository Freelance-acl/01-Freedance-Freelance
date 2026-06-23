package com.team01.freelance.contract.dto;

public class UserContractSummaryDTO {

    private Long userId;
    private String name;
    private Long totalContracts;
    private Long completedContracts;
    private Long terminatedContracts;
    private Double totalEarnings;
    private Double averageContractValue;

    public UserContractSummaryDTO() {}

    public UserContractSummaryDTO(Long userId, String name, Long totalContracts,
                                  Long completedContracts, Long terminatedContracts,
                                  Double totalEarnings, Double averageContractValue) {
        this.userId = userId;
        this.name = name;
        this.totalContracts = totalContracts;
        this.completedContracts = completedContracts;
        this.terminatedContracts = terminatedContracts;
        this.totalEarnings = totalEarnings;
        this.averageContractValue = averageContractValue;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long userId;
        private String name;
        private Long totalContracts;
        private Long completedContracts;
        private Long terminatedContracts;
        private Double totalEarnings;
        private Double averageContractValue;

        public Builder userId(Long userId) { this.userId = userId; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder totalContracts(Long totalContracts) { this.totalContracts = totalContracts; return this; }
        public Builder completedContracts(Long completedContracts) { this.completedContracts = completedContracts; return this; }
        public Builder terminatedContracts(Long terminatedContracts) { this.terminatedContracts = terminatedContracts; return this; }
        public Builder totalEarnings(Double totalEarnings) { this.totalEarnings = totalEarnings; return this; }
        public Builder averageContractValue(Double averageContractValue) { this.averageContractValue = averageContractValue; return this; }

        public UserContractSummaryDTO build() {
            return new UserContractSummaryDTO(userId, name, totalContracts, completedContracts,
                    terminatedContracts, totalEarnings, averageContractValue);
        }
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getTotalContracts() { return totalContracts; }
    public void setTotalContracts(Long totalContracts) { this.totalContracts = totalContracts; }

    public Long getCompletedContracts() { return completedContracts; }
    public void setCompletedContracts(Long completedContracts) { this.completedContracts = completedContracts; }

    public Long getTerminatedContracts() { return terminatedContracts; }
    public void setTerminatedContracts(Long terminatedContracts) { this.terminatedContracts = terminatedContracts; }

    public Double getTotalEarnings() { return totalEarnings; }
    public void setTotalEarnings(Double totalEarnings) { this.totalEarnings = totalEarnings; }

    public Double getAverageContractValue() { return averageContractValue; }
    public void setAverageContractValue(Double averageContractValue) { this.averageContractValue = averageContractValue; }
}
