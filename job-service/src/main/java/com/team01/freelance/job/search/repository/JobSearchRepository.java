package com.team01.freelance.job.search.repository;

import com.team01.freelance.job.search.document.JobSearchDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * Spring Data repository for {@link JobSearchDocument} persistence in Elasticsearch.
 */
public interface JobSearchRepository extends ElasticsearchRepository<JobSearchDocument, Long> {
}
