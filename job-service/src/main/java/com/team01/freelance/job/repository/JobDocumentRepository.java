package com.team01.freelance.job.repository;

/**
 * Disabled for S2-F11 startup safety.
 * Job indexing uses ElasticsearchOperations in JobIndexService so Elasticsearch
 * index checks happen only when POST /api/jobs/{id}/index is called.
 */
public interface JobDocumentRepository {
}
