package com.team01.freelance.contract.dto;

public class ContractSummaryDTO {
    private Long contractId;
    private String freelancerName;
    private String jobTitle;
    private Double agreedAmount;
    private String status;
    private Long durationDays;

    public ContractSummaryDTO() {
    }

    public ContractSummaryDTO(
            Long contractId,
            String freelancerName,
            String jobTitle,
            Double agreedAmount,
            String status,
            Long durationDays
    ) {
        this.contractId = contractId;
        this.freelancerName = freelancerName;
        this.jobTitle = jobTitle;
        this.agreedAmount = agreedAmount;
        this.status = status;
        this.durationDays = durationDays;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getContractId() {
        return contractId;
    }

    public void setContractId(Long contractId) {
        this.contractId = contractId;
    }

    public String getFreelancerName() {
        return freelancerName;
    }

    public void setFreelancerName(String freelancerName) {
        this.freelancerName = freelancerName;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public Double getAgreedAmount() {
        return agreedAmount;
    }

    public void setAgreedAmount(Double agreedAmount) {
        this.agreedAmount = agreedAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getDurationDays() {
        return durationDays;
    }

    public void setDurationDays(Long durationDays) {
        this.durationDays = durationDays;
    }

    public static final class Builder {
        private final ContractSummaryDTO dto = new ContractSummaryDTO();

        public Builder contractId(Long contractId) {
            dto.setContractId(contractId);
            return this;
        }

        public Builder freelancerName(String freelancerName) {
            dto.setFreelancerName(freelancerName);
            return this;
        }

        public Builder jobTitle(String jobTitle) {
            dto.setJobTitle(jobTitle);
            return this;
        }

        public Builder agreedAmount(Double agreedAmount) {
            dto.setAgreedAmount(agreedAmount);
            return this;
        }

        public Builder status(String status) {
            dto.setStatus(status);
            return this;
        }

        public Builder durationDays(Long durationDays) {
            dto.setDurationDays(durationDays);
            return this;
        }

        public ContractSummaryDTO build() {
            return dto;
        }
    }
}
