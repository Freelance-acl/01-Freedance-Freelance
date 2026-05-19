package com.team01.freelance.job.model;

import java.util.List;

public class JobAttachmentAlertDTO {

    private Long jobId;
    private String jobTitle;
    private JobStatus jobStatus;
    private List<JobAttachment> expiredAttachments;
    private Integer expiredCount;

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public JobStatus getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(JobStatus jobStatus) {
        this.jobStatus = jobStatus;
    }

    public List<JobAttachment> getExpiredAttachments() {
        return expiredAttachments;
    }

    public void setExpiredAttachments(List<JobAttachment> expiredAttachments) {
        this.expiredAttachments = expiredAttachments;
    }

    public Integer getExpiredCount() {
        return expiredCount;
    }

    public void setExpiredCount(Integer expiredCount) {
        this.expiredCount = expiredCount;
    }
}
