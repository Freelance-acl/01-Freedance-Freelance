package com.team01.freelance.job.search.document;

import com.team01.freelance.job.model.JobCategory;
import com.team01.freelance.job.model.JobStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * Elasticsearch document stored in the {@code jobs} index for full-text search.
 */
@Document(indexName = "jobs")
public class JobSearchDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Keyword)
    private JobCategory category;

    @Field(type = FieldType.Double)
    private Double budgetMin;

    @Field(type = FieldType.Double)
    private Double budgetMax;

    @Field(type = FieldType.Double)
    private Double rating;

    @Field(type = FieldType.Keyword)
    private JobStatus status;

    /** Default constructor required by Spring Data Elasticsearch. */
    public JobSearchDocument() {
    }

    /**
     * Creates a search document from indexed job fields.
     */
    public JobSearchDocument(Long id, String title, String description, JobCategory category,
                             Double budgetMin, Double budgetMax, Double rating, JobStatus status) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.budgetMin = budgetMin;
        this.budgetMax = budgetMax;
        this.rating = rating;
        this.status = status;
    }

    /** @return job identifier */
    public Long getId() {
        return id;
    }

    /** @param id job identifier */
    public void setId(Long id) {
        this.id = id;
    }

    /** @return searchable job title */
    public String getTitle() {
        return title;
    }

    /** @param title searchable job title */
    public void setTitle(String title) {
        this.title = title;
    }

    /** @return searchable job description */
    public String getDescription() {
        return description;
    }

    /** @param description searchable job description */
    public void setDescription(String description) {
        this.description = description;
    }

    /** @return indexed job category */
    public JobCategory getCategory() {
        return category;
    }

    /** @param category indexed job category */
    public void setCategory(JobCategory category) {
        this.category = category;
    }

    /** @return minimum budget used for range filtering */
    public Double getBudgetMin() {
        return budgetMin;
    }

    /** @param budgetMin minimum budget used for range filtering */
    public void setBudgetMin(Double budgetMin) {
        this.budgetMin = budgetMin;
    }

    /** @return maximum budget used for range filtering */
    public Double getBudgetMax() {
        return budgetMax;
    }

    /** @param budgetMax maximum budget used for range filtering */
    public void setBudgetMax(Double budgetMax) {
        this.budgetMax = budgetMax;
    }

    /** @return indexed job rating */
    public Double getRating() {
        return rating;
    }

    /** @param rating indexed job rating */
    public void setRating(Double rating) {
        this.rating = rating;
    }

    /** @return indexed job status */
    public JobStatus getStatus() {
        return status;
    }

    /** @param status indexed job status */
    public void setStatus(JobStatus status) {
        this.status = status;
    }
}
