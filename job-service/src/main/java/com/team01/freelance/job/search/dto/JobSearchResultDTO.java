package com.team01.freelance.job.search.dto;

import com.team01.freelance.job.model.JobCategory;
import com.team01.freelance.job.model.JobStatus;

public class JobSearchResultDTO {

    private Long id;
    private String title;
    private String description;
    private JobCategory category;
    private Double budgetMin;
    private Double budgetMax;
    private Double rating;
    private JobStatus status;
    private Float relevanceScore;

    public JobSearchResultDTO() {
    }

    public JobSearchResultDTO(Long id, String title, String description, JobCategory category,
                              Double budgetMin, Double budgetMax, Double rating, JobStatus status,
                              Float relevanceScore) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.budgetMin = budgetMin;
        this.budgetMax = budgetMax;
        this.rating = rating;
        this.status = status;
        this.relevanceScore = relevanceScore;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public JobCategory getCategory() {
        return category;
    }

    public void setCategory(JobCategory category) {
        this.category = category;
    }

    public Double getBudgetMin() {
        return budgetMin;
    }

    public void setBudgetMin(Double budgetMin) {
        this.budgetMin = budgetMin;
    }

    public Double getBudgetMax() {
        return budgetMax;
    }

    public void setBudgetMax(Double budgetMax) {
        this.budgetMax = budgetMax;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public Float getRelevanceScore() {
        return relevanceScore;
    }

    public void setRelevanceScore(Float relevanceScore) {
        this.relevanceScore = relevanceScore;
    }
}
