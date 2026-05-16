package com.team01.freelance.job.model;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * DTO for closing a job.
 */
public class JobCloseRequest {

    @JsonAlias("status")
    private String status;

    public JobCloseRequest() {
    }

    public JobCloseRequest(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "JobCloseRequest{" +
                "status='" + status + '\'' +
                '}';
    }
}
