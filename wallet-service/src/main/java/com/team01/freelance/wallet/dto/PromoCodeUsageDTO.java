package com.team01.freelance.wallet.dto;

public class PromoCodeUsageDTO {

    public Long promoCodeId;
    public String code;
    public String discountType;
    public Double discountValue;

    public Integer timesUsed;
    public Double totalDiscountGiven;

    public Boolean active;
    public Boolean expired;
}
