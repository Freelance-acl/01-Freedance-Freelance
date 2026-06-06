package com.team01.freelance.job.search.service;

import com.team01.freelance.job.model.Job;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Test-profile stub that skips Elasticsearch indexing side effects.
 */
@Service
@Profile("test")
public class NoOpJobSearchIndexService implements JobSearchIndexOperations {

    /** No-op in tests. */
    @Override
    public void index(Job job) {
    }

    /** No-op in tests. */
    @Override
    public void delete(Long jobId) {
    }

    /** No-op in tests. */
    @Override
    public void deleteAll() {
    }
}
