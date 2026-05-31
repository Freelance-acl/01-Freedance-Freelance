package com.team01.freelance.wallet.dto;

public class CategoryRevenueDTO {

    private String category;
    private Double platformFeeRevenue;
    private Double netPayoutRevenue;
    private Double totalRevenue;
    private Long payoutCount;

    public CategoryRevenueDTO(String category, Double platformFeeRevenue, Double netPayoutRevenue,
                              Double totalRevenue, Long payoutCount) {
        this.category = category;
        this.platformFeeRevenue = platformFeeRevenue;
        this.netPayoutRevenue = netPayoutRevenue;
        this.totalRevenue = totalRevenue;
        this.payoutCount = payoutCount;
    }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Double getPlatformFeeRevenue() { return platformFeeRevenue; }
    public void setPlatformFeeRevenue(Double platformFeeRevenue) { this.platformFeeRevenue = platformFeeRevenue; }

    public Double getNetPayoutRevenue() { return netPayoutRevenue; }
    public void setNetPayoutRevenue(Double netPayoutRevenue) { this.netPayoutRevenue = netPayoutRevenue; }

    public Double getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(Double totalRevenue) { this.totalRevenue = totalRevenue; }

    public Long getPayoutCount() { return payoutCount; }
    public void setPayoutCount(Long payoutCount) { this.payoutCount = payoutCount; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String category;
        private Double platformFeeRevenue;
        private Double netPayoutRevenue;
        private Double totalRevenue;
        private Long payoutCount;

        public Builder category(String category) { this.category = category; return this; }
        public Builder platformFeeRevenue(Double platformFeeRevenue) { this.platformFeeRevenue = platformFeeRevenue; return this; }
        public Builder netPayoutRevenue(Double netPayoutRevenue) { this.netPayoutRevenue = netPayoutRevenue; return this; }
        public Builder totalRevenue(Double totalRevenue) { this.totalRevenue = totalRevenue; return this; }
        public Builder payoutCount(Long payoutCount) { this.payoutCount = payoutCount; return this; }

        public CategoryRevenueDTO build() {
            return new CategoryRevenueDTO(category, platformFeeRevenue, netPayoutRevenue, totalRevenue, payoutCount);
        }
    }
}
