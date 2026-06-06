package com.team01.freelance.wallet.dto;

import com.team01.freelance.wallet.model.PayoutMethod;
import java.io.Serializable;

public class PayoutMethodDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private PayoutMethod method;
    private long successCount;
    private long failureCount;
    private double successRate;
    private double totalAmount;

    // Default Constructor
    public PayoutMethodDTO() {
    }

    // Full Constructor
    public PayoutMethodDTO(PayoutMethod method, long successCount, long failureCount, double successRate, double totalAmount) {
        this.method = method;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.successRate = successRate;
        this.totalAmount = totalAmount;
    }

    // Getters and Setters
    public PayoutMethod method() {
        return method;
    }

    public PayoutMethod getMethod() {
        return method;
    }

    public void setMethod(PayoutMethod method) {
        this.method = method;
    }

    public long getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(long successCount) {
        this.successCount = successCount;
    }

    public long getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(long failureCount) {
        this.failureCount = failureCount;
    }

    public double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(double successRate) {
        this.successRate = successRate;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }
}