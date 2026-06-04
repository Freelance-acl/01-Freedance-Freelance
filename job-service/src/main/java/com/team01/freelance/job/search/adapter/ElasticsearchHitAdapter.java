package com.team01.freelance.job.search.adapter;

import com.team01.freelance.job.search.document.JobSearchDocument;
import com.team01.freelance.job.search.dto.JobSearchResultDTO;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.stereotype.Component;

@Component
public class ElasticsearchHitAdapter {

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
