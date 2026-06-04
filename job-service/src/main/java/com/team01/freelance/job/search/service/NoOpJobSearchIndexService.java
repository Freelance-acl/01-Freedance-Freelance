package com.team01.freelance.job.search.service;

import com.team01.freelance.job.model.Job;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("test")
public class NoOpJobSearchIndexService implements JobSearchIndexOperations {

    @Override
    public void index(Job job) {
    }

    @Override
    public void delete(Long jobId) {
    }

    @Override
    public void deleteAll() {
    }
}
