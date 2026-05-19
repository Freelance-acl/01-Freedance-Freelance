package com.team01.freelance.user.dto;

import java.math.BigDecimal;

public class TopFreelancerDTO {
    private Long userId;
    private String name;
    private BigDecimal totalEarnings;
    private Long contractCount;

    public TopFreelancerDTO(Long userId, String name, BigDecimal totalEarnings, Long contractCount) {
        this.userId = userId;
        this.name = name;
        this.totalEarnings = totalEarnings;
        this.contractCount = contractCount;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getTotalEarnings() {
        return totalEarnings;
    }

    public void setTotalEarnings(BigDecimal totalEarnings) {
        this.totalEarnings = totalEarnings;
    }

    public Long getContractCount() {
        return contractCount;
    }

    public void setContractCount(Long contractCount) {
        this.contractCount = contractCount;
    }
}
