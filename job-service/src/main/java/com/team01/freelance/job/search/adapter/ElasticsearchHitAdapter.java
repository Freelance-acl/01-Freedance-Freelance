package com.team01.freelance.job.search.adapter;

import com.team01.freelance.job.search.document.JobSearchDocument;
import com.team01.freelance.job.search.dto.JobSearchResultDTO;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.stereotype.Component;

/**
 * Maps Elasticsearch {@link SearchHit} instances to {@link JobSearchResultDTO}.
 */
@Component
public class ElasticsearchHitAdapter {

    /**
     * Converts an Elasticsearch search hit into an API search result DTO.
     *
     * @param hit Elasticsearch hit containing a {@link JobSearchDocument}
     * @return mapped search result including relevance score
     */
    public JobSearchResultDTO toDto(SearchHit<JobSearchDocument> hit) {
        JobSearchDocument document = hit.getContent();
        return new JobSearchResultDTO(
                document.getId(),
                document.getTitle(),
                document.getDescription(),
                document.getCategory(),
                document.getBudgetMin(),
                document.getBudgetMax(),
                document.getRating(),
                document.getStatus(),
                hit.getScore()
        );
    }
}
