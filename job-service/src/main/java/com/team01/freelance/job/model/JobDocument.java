package com.team01.freelance.job.model;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * Elasticsearch document for job full-text search.
 * Contains denormalized job data for efficient searching.
 * Mapped to "jobs" index in Elasticsearch.
 */
public class JobDocument {

    private Long id;

    private String title;

    private String description;

    private String category;

    @JsonAlias({"budgetMin", "budget_min"})
    private Double budgetMin;

    @JsonAlias({"budgetMax", "budget_max"})
    private Double budgetMax;

    private Double rating;

    private String status;

    public JobDocument() {
    }

    public JobDocument(Long id, String title, String description, String category,
                       Double budgetMin, Double budgetMax, Double rating, String status) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.budgetMin = budgetMin;
        this.budgetMax = budgetMax;
        this.rating = rating;
        this.status = status;
    }

    // Getters and Setters
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "JobDocument{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", category='" + category + '\'' +
                ", budgetMin=" + budgetMin +
                ", budgetMax=" + budgetMax +
                ", rating=" + rating +
                ", status='" + status + '\'' +
                '}';
    }
}
