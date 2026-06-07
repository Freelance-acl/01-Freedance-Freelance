package com.team01.freelance.job.search.service;

import com.team01.freelance.job.model.JobCategory;
import com.team01.freelance.job.model.JobStatus;
import com.team01.freelance.job.search.dto.JobSearchResultDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Test-profile stub that disables Elasticsearch full-text search.
 */
@Service
@Profile("test")
public class NoOpJobFullTextSearchService implements JobFullTextSearchOperations {

    /**
     * Returns an empty page because search is disabled in the test profile.
     */
    @Override
    public Page<JobSearchResultDTO> search(String query, JobCategory category, JobStatus status,
                                           Double minBudget, Double maxBudget, Pageable pageable) {
        return Page.empty(pageable);
    }
}
