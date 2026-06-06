package com.team01.freelance.job.search.service;

import com.team01.freelance.job.model.Job;
import com.team01.freelance.job.search.document.JobSearchDocument;
import com.team01.freelance.job.search.repository.JobSearchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Keeps the Elasticsearch jobs index synchronized with PostgreSQL job writes.
 */
@Service
@Profile("!test")
public class JobSearchIndexService implements JobSearchIndexOperations {

    private static final Logger log = LoggerFactory.getLogger(JobSearchIndexService.class);
    private static final String SEARCH_CACHE = "S2-F10";

    private final JobSearchRepository jobSearchRepository;

    /**
     * @param jobSearchRepository repository used to persist search documents
     */
    public JobSearchIndexService(JobSearchRepository jobSearchRepository) {
        this.jobSearchRepository = jobSearchRepository;
    }

    /**
     * Indexes the job and evicts cached full-text search results.
     */
    @CacheEvict(value = SEARCH_CACHE, allEntries = true)
    @Override
    public void index(Job job) {
        if (job == null || job.getId() == null) {
            return;
        }
        try {
            jobSearchRepository.save(toDocument(job));
        } catch (RuntimeException ex) {
            log.warn("Failed to index job {} in Elasticsearch: {}", job.getId(), ex.getMessage());
        }
    }

    /**
     * Deletes the indexed job and evicts cached full-text search results.
     */
    @CacheEvict(value = SEARCH_CACHE, allEntries = true)
    @Override
    public void delete(Long jobId) {
        if (jobId == null) {
            return;
        }
        try {
            jobSearchRepository.deleteById(jobId);
        } catch (RuntimeException ex) {
            log.warn("Failed to delete job {} from Elasticsearch: {}", jobId, ex.getMessage());
        }
    }

    /**
     * Clears the Elasticsearch jobs index and evicts cached full-text search results.
     */
    @CacheEvict(value = SEARCH_CACHE, allEntries = true)
    @Override
    public void deleteAll() {
        try {
            jobSearchRepository.deleteAll();
        } catch (RuntimeException ex) {
            log.warn("Failed to delete all jobs from Elasticsearch: {}", ex.getMessage());
        }
    }

    /**
     * Builds the Elasticsearch document representation of a job.
     */
    private JobSearchDocument toDocument(Job job) {
        return new JobSearchDocument(
                job.getId(),
                job.getTitle(),
                job.getDescription(),
                job.getCategory(),
                job.getBudgetMin(),
                job.getBudgetMax(),
                job.getRating(),
                job.getStatus()
        );
    }
}
