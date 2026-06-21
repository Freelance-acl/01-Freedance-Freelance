package com.team01.freelance.wallet.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ContractDTO {

    private Long id;
    private String status;
    private Long clientId;
    private Double agreedAmount;
    private Long proposalId;
    private Long jobId;

    public ContractDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public Double getAgreedAmount() { return agreedAmount; }
    public void setAgreedAmount(Double agreedAmount) { this.agreedAmount = agreedAmount; }

    public Long getProposalId() { return proposalId; }
    public void setProposalId(Long proposalId) { this.proposalId = proposalId; }

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
}