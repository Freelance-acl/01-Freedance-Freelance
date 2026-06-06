package com.team01.freelance.contract.dto;

public class BatchContractStatusUpdateResponse {
    private long count;

    public BatchContractStatusUpdateResponse(long count) {
        this.count = count;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
