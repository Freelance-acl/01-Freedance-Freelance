package com.team01.freelance.job.model;

import com.fasterxml.jackson.annotation.JsonAlias;

public class JobCloseRequest {

    @JsonAlias("status")
    private String status;

    public JobCloseRequest() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
