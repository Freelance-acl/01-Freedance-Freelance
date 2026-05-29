package com.team01.freelance.wallet.event;

import com.team01.freelance.common.event.MongoEvent;
import com.team01.freelance.wallet.model.PayoutMethod;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Document(collection = "payout_audit_trail")
public class PayoutAuditEvent implements MongoEvent {

    @Id
    private String id;

    private Long payoutId;
    private String action;
    private LocalDateTime timestamp;
    private PayoutMethod method;
    private Double amount;
    private Map<String, Object> details = new HashMap<>();

    public PayoutAuditEvent() {
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getPayoutId() {
        return payoutId;
    }

    public void setPayoutId(Long payoutId) {
        this.payoutId = payoutId;
    }

    @Override
    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    @Override
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public PayoutMethod getMethod() {
        return method;
    }

    public void setMethod(PayoutMethod method) {
        this.method = method;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    @Override
    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details != null ? new HashMap<>(details) : new HashMap<>();
    }
}

