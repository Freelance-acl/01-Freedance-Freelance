package com.team01.freelance.job.search.service;

import com.team01.freelance.job.model.Job;

/**
 * Contract for synchronizing PostgreSQL jobs into the Elasticsearch search index.
 */
public interface JobSearchIndexOperations {

    /**
     * Creates or updates the Elasticsearch document for the given job.
     *
     * @param job job entity to index
     */
    void index(Job job);

    /**
     * Removes a job document from the Elasticsearch index.
     *
     * @param jobId identifier of the job to delete
     */
    void delete(Long jobId);

    /** Removes all job documents from the Elasticsearch index. */
    void deleteAll();
}
