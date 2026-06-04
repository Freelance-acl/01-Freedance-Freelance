package com.team01.freelance.job.search.repository;

import com.team01.freelance.job.search.document.JobSearchDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobSearchRepository extends ElasticsearchRepository<JobSearchDocument, Long> {
}
