package com.team01.freelance.job.search.service;

import com.team01.freelance.job.model.JobCategory;
import com.team01.freelance.job.model.JobStatus;
import com.team01.freelance.job.search.adapter.ElasticsearchHitAdapter;
import com.team01.freelance.job.search.document.JobSearchDocument;
import com.team01.freelance.job.search.dto.JobSearchResultDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Profile("!test")
public class JobFullTextSearchService implements JobFullTextSearchOperations {

    private static final Logger log = LoggerFactory.getLogger(JobFullTextSearchService.class);
    private static final String SEARCH_CACHE = "S2-F10";

    private final ElasticsearchOperations elasticsearchOperations;
    private final ElasticsearchHitAdapter hitAdapter;

    public JobFullTextSearchService(ElasticsearchOperations elasticsearchOperations,
                                    ElasticsearchHitAdapter hitAdapter) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.hitAdapter = hitAdapter;
    }

    @Cacheable(value = SEARCH_CACHE,
            key = "#query + '|' + #category + '|' + #status + '|' + #minBudget + '|' + #maxBudget + '|' + #pageable")
    @Override
    public Page<JobSearchResultDTO> search(String query, JobCategory category, JobStatus status,
                                           Double minBudget, Double maxBudget, Pageable pageable) {
        Pageable relevancePageable = pageable.isPaged()
                ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize())
                : pageable;
        if (query == null || query.trim().isEmpty()) {
            return Page.empty(relevancePageable);
        }
        if (minBudget != null && maxBudget != null && minBudget > maxBudget) {
            throw new IllegalArgumentException("minBudget cannot be greater than maxBudget");
        }

        NativeQuery searchQuery = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> {
                    b.must(m -> m.multiMatch(mm -> mm
                            .query(query.trim())
                            .fields("title^2", "description")));
                    if (category != null) {
                        b.filter(f -> f.term(t -> t.field("category").value(category.name())));
                    }
                    if (status != null) {
                        b.filter(f -> f.term(t -> t.field("status").value(status.name())));
                    }
                    if (minBudget != null || maxBudget != null) {
                        b.filter(f -> f.range(r -> r.number(n -> {
                            n.field("budgetMax");
                            if (minBudget != null) {
                                n.gte(minBudget);
                            }
                            return n;
                        })));
                        b.filter(f -> f.range(r -> r.number(n -> {
                            n.field("budgetMin");
                            if (maxBudget != null) {
                                n.lte(maxBudget);
                            }
                            return n;
                        })));
                    }
                    return b;
                }))
                .withPageable(relevancePageable)
                .build();

        try {
            SearchHits<JobSearchDocument> hits = elasticsearchOperations.search(searchQuery, JobSearchDocument.class);
            List<JobSearchResultDTO> content = hits.stream()
                    .map(this::adapt)
                    .toList();
            return new PageImpl<>(content, relevancePageable, hits.getTotalHits());
        } catch (RuntimeException ex) {
            log.warn("Elasticsearch full-text job search failed: {}", ex.getMessage());
            return Page.empty(relevancePageable);
        }
    }

    private JobSearchResultDTO adapt(SearchHit<JobSearchDocument> hit) {
        return hitAdapter.toDto(hit);
    }
}
