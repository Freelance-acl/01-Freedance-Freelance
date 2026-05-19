package com.team01.freelance.contract.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.team01.freelance.contract.model.ContractStatus;

public class BatchContractStatusUpdateRequest {
    @JsonAlias({"contractId", "contract_id"})
    private Long contractId;

    private ContractStatus status;

    public BatchContractStatusUpdateRequest() {
    }

    public BatchContractStatusUpdateRequest(Long contractId, ContractStatus status) {
        this.contractId = contractId;
        this.status = status;
    }

    public Long getContractId() {
        return contractId;
    }

    public void setContractId(Long contractId) {
        this.contractId = contractId;
    }

    public ContractStatus getStatus() {
        return status;
    }

    public void setStatus(ContractStatus status) {
        this.status = status;
    }
}
