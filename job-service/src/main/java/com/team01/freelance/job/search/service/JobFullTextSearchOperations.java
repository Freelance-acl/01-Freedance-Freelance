package com.team01.freelance.job.search.service;

import com.team01.freelance.job.model.JobCategory;
import com.team01.freelance.job.model.JobStatus;
import com.team01.freelance.job.search.dto.JobSearchResultDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface JobFullTextSearchOperations {

    Page<JobSearchResultDTO> search(String query, JobCategory category, JobStatus status,
                                    Double minBudget, Double maxBudget, Pageable pageable);
}
