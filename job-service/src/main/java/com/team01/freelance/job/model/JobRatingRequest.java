package com.team01.freelance.job.model;

import com.fasterxml.jackson.annotation.JsonAlias;

public class JobRatingRequest {

    @JsonAlias({"contractId", "contract_id"})
    private Long contractId;

    private Double rating;

    public Long getContractId() {
        return contractId;
    }

    public void setContractId(Long contractId) {
        this.contractId = contractId;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }
}
