package com.team01.freelance.wallet.dto;

public class RevenueReportDTO {

    private Double totalRevenue;
    private Long totalTransactions;
    private Double averagePayout;
    private Double refundedAmount;
    private Long refundCount;

    public RevenueReportDTO(Double totalRevenue, Long totalTransactions, Double averagePayout,
                            Double refundedAmount, Long refundCount) {
        this.totalRevenue = totalRevenue;
        this.totalTransactions = totalTransactions;
        this.averagePayout = averagePayout;
        this.refundedAmount = refundedAmount;
        this.refundCount = refundCount;
    }

    public Double getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(Double totalRevenue) { this.totalRevenue = totalRevenue; }

    public Long getTotalTransactions() { return totalTransactions; }
    public void setTotalTransactions(Long totalTransactions) { this.totalTransactions = totalTransactions; }

    public Double getAveragePayout() { return averagePayout; }
    public void setAveragePayout(Double averagePayout) { this.averagePayout = averagePayout; }

    public Double getRefundedAmount() { return refundedAmount; }
    public void setRefundedAmount(Double refundedAmount) { this.refundedAmount = refundedAmount; }

    public Long getRefundCount() { return refundCount; }
    public void setRefundCount(Long refundCount) { this.refundCount = refundCount; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Double totalRevenue;
        private Long totalTransactions;
        private Double averagePayout;
        private Double refundedAmount;
        private Long refundCount;

        public Builder totalRevenue(Double totalRevenue) { this.totalRevenue = totalRevenue; return this; }
        public Builder totalTransactions(Long totalTransactions) { this.totalTransactions = totalTransactions; return this; }
        public Builder averagePayout(Double averagePayout) { this.averagePayout = averagePayout; return this; }
        public Builder refundedAmount(Double refundedAmount) { this.refundedAmount = refundedAmount; return this; }
        public Builder refundCount(Long refundCount) { this.refundCount = refundCount; return this; }

        public RevenueReportDTO build() {
            return new RevenueReportDTO(totalRevenue, totalTransactions, averagePayout, refundedAmount, refundCount);
        }
    }
}