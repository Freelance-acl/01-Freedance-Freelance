package com.team01.freelance.job.model;

import com.fasterxml.jackson.annotation.JsonAlias;

public class JobAttachmentVerificationRequest {

    @JsonAlias({"verifiedBy", "verified_by"})
    private Long verifiedBy;

    public Long getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(Long verifiedBy) {
        this.verifiedBy = verifiedBy;
    }
}
