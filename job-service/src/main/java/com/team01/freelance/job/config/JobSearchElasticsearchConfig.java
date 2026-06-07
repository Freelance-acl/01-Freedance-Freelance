package com.team01.freelance.job.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Enables Elasticsearch repositories for job full-text search outside the test profile.
 */
@Configuration
@Profile("!test")
@EnableElasticsearchRepositories(basePackages = "com.team01.freelance.job.search.repository")
public class JobSearchElasticsearchConfig {
}
