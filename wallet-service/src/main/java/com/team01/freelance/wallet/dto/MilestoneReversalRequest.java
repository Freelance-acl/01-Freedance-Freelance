package com.team01.freelance.wallet.dto;

public class MilestoneReversalRequest {

    private String reason;
    private String reversalScope;

    public MilestoneReversalRequest() {
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getReversalScope() {
        return reversalScope;
    }

    public void setReversalScope(String reversalScope) {
        this.reversalScope = reversalScope;
    }
}
