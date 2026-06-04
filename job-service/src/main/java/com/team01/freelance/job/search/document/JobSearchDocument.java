package com.team01.freelance.job.search.document;

import com.team01.freelance.job.model.JobCategory;
import com.team01.freelance.job.model.JobStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

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

    public JobSearchDocument() {
    }

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
}
