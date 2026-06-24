package com.team01.freelance.job.search.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team01.freelance.job.model.JobCategory;
import com.team01.freelance.job.model.JobStatus;
import com.team01.freelance.job.search.adapter.ElasticsearchHitAdapter;
import com.team01.freelance.job.search.document.JobSearchDocument;
import com.team01.freelance.job.search.dto.JobSearchResultDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Elasticsearch implementation of full-text job search with Redis-backed caching.
 */
@Service
@Profile("!test")
public class JobFullTextSearchService implements JobFullTextSearchOperations {

    private static final Logger log = LoggerFactory.getLogger(JobFullTextSearchService.class);
    private static final String SEARCH_CACHE_PREFIX = "S2-F10::full-text::";
    private static final Duration SEARCH_CACHE_TTL = Duration.ofMinutes(5);

    private final ElasticsearchOperations elasticsearchOperations;
    private final ElasticsearchHitAdapter hitAdapter;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * @param elasticsearchOperations Spring Data Elasticsearch operations facade
     * @param hitAdapter adapter that maps search hits to DTOs
     */
    public JobFullTextSearchService(ElasticsearchOperations elasticsearchOperations,
                                    ElasticsearchHitAdapter hitAdapter,
                                    StringRedisTemplate redisTemplate,
                                    ObjectMapper objectMapper) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.hitAdapter = hitAdapter;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Executes a relevance-ranked Elasticsearch query and caches the response for five minutes.
     */
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

        String cacheKey = cacheKey(query, category, status, minBudget, maxBudget, relevancePageable);
        Optional<Page<JobSearchResultDTO>> cached = readCachedPage(cacheKey, relevancePageable);
        if (cached.isPresent()) {
            return cached.get();
        }

        Page<JobSearchResultDTO> result = executeSearch(query, category, status, minBudget, maxBudget, relevancePageable);
        writeCachedPage(cacheKey, result);
        return result;
    }

    private Page<JobSearchResultDTO> executeSearch(String query, JobCategory category, JobStatus status,
                                                   Double minBudget, Double maxBudget, Pageable relevancePageable) {
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

    private Optional<Page<JobSearchResultDTO>> readCachedPage(String cacheKey, Pageable pageable) {
        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson == null || cachedJson.isBlank()) {
                return Optional.empty();
            }
            CachedSearchPage cached = objectMapper.readValue(cachedJson, CachedSearchPage.class);
            return Optional.of(cached.toPage(pageable));
        } catch (RuntimeException | JsonProcessingException ex) {
            log.warn("Failed to read S2-F10 Redis cache entry {}: {}", cacheKey, ex.getMessage());
            redisTemplate.delete(cacheKey);
            return Optional.empty();
        }
    }

    private void writeCachedPage(String cacheKey, Page<JobSearchResultDTO> page) {
        try {
            redisTemplate.opsForValue().set(
                    cacheKey,
                    objectMapper.writeValueAsString(CachedSearchPage.from(page)),
                    SEARCH_CACHE_TTL);
        } catch (RuntimeException | JsonProcessingException ex) {
            log.warn("Failed to write S2-F10 Redis cache entry {}: {}", cacheKey, ex.getMessage());
        }
    }

    private String cacheKey(String query, JobCategory category, JobStatus status,
                            Double minBudget, Double maxBudget, Pageable pageable) {
        return SEARCH_CACHE_PREFIX
                + query.trim().toLowerCase()
                + "|" + (category == null ? "ALL" : category.name())
                + "|" + (status == null ? "ALL" : status.name())
                + "|" + (minBudget == null ? "NONE" : minBudget)
                + "|" + (maxBudget == null ? "NONE" : maxBudget)
                + "|" + (pageable.isPaged() ? pageable.getPageNumber() : 0)
                + "|" + (pageable.isPaged() ? pageable.getPageSize() : "ALL");
    }

    /**
     * Maps a single Elasticsearch hit to a search result DTO.
     */
    private JobSearchResultDTO adapt(SearchHit<JobSearchDocument> hit) {
        return hitAdapter.toDto(hit);
    }

    static class CachedSearchPage {
        private List<JobSearchResultDTO> content = new ArrayList<>();
        private long totalElements;
        private int pageNumber;
        private int pageSize;

        static CachedSearchPage from(Page<JobSearchResultDTO> page) {
            CachedSearchPage cached = new CachedSearchPage();
            cached.content = new ArrayList<>(page.getContent());
            cached.totalElements = page.getTotalElements();
            cached.pageNumber = page.getPageable().isPaged() ? page.getNumber() : 0;
            cached.pageSize = page.getPageable().isPaged() ? page.getSize() : Math.max(page.getContent().size(), 1);
            return cached;
        }

        Page<JobSearchResultDTO> toPage(Pageable requestedPageable) {
            Pageable cachedPageable = requestedPageable.isPaged()
                    ? requestedPageable
                    : PageRequest.of(pageNumber, Math.max(pageSize, 1));
            return new PageImpl<>(content, cachedPageable, totalElements);
        }

        public List<JobSearchResultDTO> getContent() {
            return content;
        }

        public void setContent(List<JobSearchResultDTO> content) {
            this.content = content == null ? new ArrayList<>() : content;
        }

        public long getTotalElements() {
            return totalElements;
        }

        public void setTotalElements(long totalElements) {
            this.totalElements = totalElements;
        }

        public int getPageNumber() {
            return pageNumber;
        }

        public void setPageNumber(int pageNumber) {
            this.pageNumber = pageNumber;
        }

        public int getPageSize() {
            return pageSize;
        }

        public void setPageSize(int pageSize) {
            this.pageSize = pageSize;
        }
    }
}
