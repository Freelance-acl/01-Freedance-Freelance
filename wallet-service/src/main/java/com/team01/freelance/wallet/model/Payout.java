package com.team01.freelance.wallet.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "payouts")
public class Payout {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contract_id", nullable = false)
    @JsonAlias({"contractId", "contract_id"})
    private Long contractId;

    @Column(name = "freelancer_id", nullable = false)
    @JsonAlias({"freelancerId", "freelancer_id"})
    private Long freelancerId;

    @Column(name = "amount", nullable = false)
    private Double amount;

    @Column(name = "method", nullable = false)
    @Enumerated(EnumType.STRING)
    private PayoutMethod method;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private PayoutStatus status;


    @Column(name = "transaction_details", columnDefinition = "jsonb")
    @JsonAlias({"transactionDetails", "transaction_details"})
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> transactionDetails;
    
    @Column(name = "created_at", nullable = false)
    @JsonAlias({"createdAt", "created_at"})
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "payout", fetch = FetchType.LAZY)
    private List<PayoutPromo> payoutPromos;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getContractId() {
        return contractId;
    }

    public void setContractId(Long contractId) {
        this.contractId = contractId;
    }

    public Long getFreelancerId() {
        return freelancerId;
    }

    public void setFreelancerId(Long freelancerId) {
        this.freelancerId = freelancerId;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public PayoutMethod getMethod() {
        return method;
    }

    public void setMethod(PayoutMethod method) {
        this.method = method;
    }

    public PayoutStatus getStatus() {
        return status;
    }

    public void setStatus(PayoutStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Map<String, Object> getTransactionDetails() {
        return transactionDetails;
    }

    public void setTransactionDetails(Map<String, Object> transactionDetails) {
        this.transactionDetails = transactionDetails;
    }

    public List<PayoutPromo> getPayoutPromos() {
        return payoutPromos;
    }

    public void setPayoutPromos(List<PayoutPromo> payoutPromos) {
        this.payoutPromos = payoutPromos;
    }
}
