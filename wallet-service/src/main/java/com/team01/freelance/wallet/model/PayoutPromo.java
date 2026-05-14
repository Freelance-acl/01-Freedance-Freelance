package com.team01.freelance.wallet.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payout_promos")
public class PayoutPromo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "discount_applied", nullable = false)
    @JsonAlias({"discountApplied", "discount_applied"})
    private Double discountApplied;

    @Column(name = "applied_at", nullable = false)
    @JsonAlias({"appliedAt", "applied_at"})
    private LocalDateTime appliedAt;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "payout_id", nullable = false)
    private Payout payout;

    @ManyToOne
    @JoinColumn(name = "promo_code_id", nullable = false)
    @JsonAlias({"promoCode", "promo_code_id"})
    private PromoCode promoCode;




    @PrePersist
    public void onCreate() {
        if (appliedAt == null) {
            appliedAt = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getDiscountApplied() {
        return discountApplied;
    }

    public void setDiscountApplied(Double discountApplied) {
        this.discountApplied = discountApplied;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(LocalDateTime appliedAt) {
        this.appliedAt = appliedAt;
    }

    public Payout getPayout() {
        return payout;
    }

    public void setPayout(Payout payout) {
        this.payout = payout;
    }

    public PromoCode getPromoCode() {
        return promoCode;
    }

    public void setPromoCode(PromoCode promoCode) {
        this.promoCode = promoCode;
    }
}

