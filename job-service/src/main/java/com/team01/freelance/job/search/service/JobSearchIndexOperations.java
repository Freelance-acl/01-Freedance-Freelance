package com.team01.freelance.job.search.service;

import com.team01.freelance.job.model.Job;

public interface JobSearchIndexOperations {

    void index(Job job);

    void delete(Long jobId);

    void deleteAll();
}
