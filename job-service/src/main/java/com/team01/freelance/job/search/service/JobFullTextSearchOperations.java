package com.team01.freelance.job.search.service;

import com.team01.freelance.job.model.JobCategory;
import com.team01.freelance.job.model.JobStatus;
import com.team01.freelance.job.search.dto.JobSearchResultDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Contract for Elasticsearch-backed full-text job search.
 */
public interface JobFullTextSearchOperations {

    /**
     * Searches indexed jobs by query text and optional filters.
     *
     * @param query free-text search terms matched against title and description
     * @param category optional category filter
     * @param status optional status filter
     * @param minBudget optional minimum budget filter
     * @param maxBudget optional maximum budget filter
     * @param pageable pagination and sort settings
     * @return a page of relevance-ranked search results
     */
    Page<JobSearchResultDTO> search(String query, JobCategory category, JobStatus status,
                                    Double minBudget, Double maxBudget, Pageable pageable);
}
