package com.team01.freelance.wallet.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.team01.freelance.wallet.model.PayoutMethod;

public class ProcessPayoutRequest {

    private PayoutMethod method;

    @JsonAlias({"accountLastFour", "account_last_four"})
    private String accountLastFour;

    public PayoutMethod getMethod() {
        return method;
    }

    public void setMethod(PayoutMethod method) {
        this.method = method;
    }

    public String getAccountLastFour() {
        return accountLastFour;
    }

    public void setAccountLastFour(String accountLastFour) {
        this.accountLastFour = accountLastFour;
    }
}
