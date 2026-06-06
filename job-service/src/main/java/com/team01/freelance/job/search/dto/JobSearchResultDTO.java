package com.team01.freelance.job.search.dto;

import com.team01.freelance.job.model.JobCategory;
import com.team01.freelance.job.model.JobStatus;

/**
 * API representation of a job returned by the full-text search endpoint.
 */
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

    /** Default constructor for JSON deserialization. */
    public JobSearchResultDTO() {
    }

    /**
     * Creates a search result with Elasticsearch relevance metadata.
     */
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

    /** @return matched job identifier */
    public Long getId() {
        return id;
    }

    /** @param id matched job identifier */
    public void setId(Long id) {
        this.id = id;
    }

    /** @return matched job title */
    public String getTitle() {
        return title;
    }

    /** @param title matched job title */
    public void setTitle(String title) {
        this.title = title;
    }

    /** @return matched job description */
    public String getDescription() {
        return description;
    }

    /** @param description matched job description */
    public void setDescription(String description) {
        this.description = description;
    }

    /** @return matched job category */
    public JobCategory getCategory() {
        return category;
    }

    /** @param category matched job category */
    public void setCategory(JobCategory category) {
        this.category = category;
    }

    /** @return matched job minimum budget */
    public Double getBudgetMin() {
        return budgetMin;
    }

    /** @param budgetMin matched job minimum budget */
    public void setBudgetMin(Double budgetMin) {
        this.budgetMin = budgetMin;
    }

    /** @return matched job maximum budget */
    public Double getBudgetMax() {
        return budgetMax;
    }

    /** @param budgetMax matched job maximum budget */
    public void setBudgetMax(Double budgetMax) {
        this.budgetMax = budgetMax;
    }

    /** @return matched job rating */
    public Double getRating() {
        return rating;
    }

    /** @param rating matched job rating */
    public void setRating(Double rating) {
        this.rating = rating;
    }

    /** @return matched job status */
    public JobStatus getStatus() {
        return status;
    }

    /** @param status matched job status */
    public void setStatus(JobStatus status) {
        this.status = status;
    }

    /** @return Elasticsearch relevance score for the hit */
    public Float getRelevanceScore() {
        return relevanceScore;
    }

    /** @param relevanceScore Elasticsearch relevance score for the hit */
    public void setRelevanceScore(Float relevanceScore) {
        this.relevanceScore = relevanceScore;
    }
}
