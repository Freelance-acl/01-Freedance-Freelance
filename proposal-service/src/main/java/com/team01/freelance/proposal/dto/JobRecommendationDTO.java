package com.team01.freelance.proposal.dto;

public class JobRecommendationDTO {
    private Long jobId;
    private String title;
    private String category;
    private long score;

    public JobRecommendationDTO() {
    }

    public JobRecommendationDTO(Long jobId, String title, String category, long score) {
        this.jobId = jobId;
        this.title = title;
        this.category = category;
        this.score = score;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public long getScore() {
        return score;
    }

    public void setScore(long score) {
        this.score = score;
    }
}
