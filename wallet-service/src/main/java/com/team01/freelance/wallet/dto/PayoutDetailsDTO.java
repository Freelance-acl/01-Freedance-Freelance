package com.team01.freelance.wallet.dto;

import com.team01.freelance.wallet.model.PayoutMethod;
import com.team01.freelance.wallet.model.PayoutStatus;

import java.util.List;
import java.util.Map;

public class PayoutDetailsDTO {

    public Long payoutId;
    public Long contractId;
    public Long freelancerId;

    public Double originalAmount;
    public PayoutMethod method;
    public PayoutStatus status;

    public Map<String, Object> transactionDetails;

    public List<AppliedPromoCodeDTO> appliedPromoCodes;

    public Double totalDiscount;
    public Double finalAmount;
}
