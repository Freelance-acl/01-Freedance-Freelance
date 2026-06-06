package com.team01.freelance.job.model;

import java.util.List;

public class JobAttachmentAlertDTO {

    private Long jobId;
    private String jobTitle;
    private JobStatus jobStatus;
    private List<JobAttachment> expiredAttachments;
    private Integer expiredCount;

    public JobAttachmentAlertDTO() {}

    public JobAttachmentAlertDTO(Long jobId, String jobTitle, JobStatus jobStatus,
                                 List<JobAttachment> expiredAttachments, Integer expiredCount) {
        this.jobId = jobId;
        this.jobTitle = jobTitle;
        this.jobStatus = jobStatus;
        this.expiredAttachments = expiredAttachments;
        this.expiredCount = expiredCount;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Long jobId;
        private String jobTitle;
        private JobStatus jobStatus;
        private List<JobAttachment> expiredAttachments;
        private Integer expiredCount;

        private Builder() {}

        public Builder jobId(Long jobId) { this.jobId = jobId; return this; }
        public Builder jobTitle(String jobTitle) { this.jobTitle = jobTitle; return this; }
        public Builder jobStatus(JobStatus jobStatus) { this.jobStatus = jobStatus; return this; }
        public Builder expiredAttachments(List<JobAttachment> expiredAttachments) { this.expiredAttachments = expiredAttachments; return this; }
        public Builder expiredCount(Integer expiredCount) { this.expiredCount = expiredCount; return this; }

        public JobAttachmentAlertDTO build() {
            return new JobAttachmentAlertDTO(jobId, jobTitle, jobStatus, expiredAttachments, expiredCount);
        }
    }

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public JobStatus getJobStatus() { return jobStatus; }
    public void setJobStatus(JobStatus jobStatus) { this.jobStatus = jobStatus; }
    public List<JobAttachment> getExpiredAttachments() { return expiredAttachments; }
    public void setExpiredAttachments(List<JobAttachment> expiredAttachments) { this.expiredAttachments = expiredAttachments; }
    public Integer getExpiredCount() { return expiredCount; }
    public void setExpiredCount(Integer expiredCount) { this.expiredCount = expiredCount; }
}
