package com.team01.freelance.job.service;

import com.team01.freelance.job.model.Job;
import com.team01.freelance.job.model.JobDocument;
import com.team01.freelance.job.observer.EntityObserver;
import com.team01.freelance.job.repository.JobRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for handling job indexing operations in Elasticsearch.
 * Responsible for creating/updating JobDocument entries based on Job data.
 * Notifies observers of INDEXED events.
 */
@Service
public class JobIndexService {

    private final JobRepository jobRepository;
    private final RestClient elasticsearchClient;
    private final List<EntityObserver> observers;

    public JobIndexService(JobRepository jobRepository,
                           @Value("${spring.elasticsearch.uris}") String elasticsearchUris,
                           List<EntityObserver> observers) {
        this.jobRepository = jobRepository;
        this.elasticsearchClient = RestClient.builder()
                .baseUrl(firstElasticsearchUri(elasticsearchUris))
                .build();
        this.observers = observers;
    }

    /**
     * Indexes a job in Elasticsearch.
     * Loads the job from PostgreSQL, creates/updates a JobDocument, and saves to Elasticsearch.
     * After successful indexing, notifies all observers of the INDEXED event.
     *
     * @param jobId the ID of the job to index
     * @return a map containing indexing status and indexed fields
     * @throws EntityNotFoundException if job is not found
     * @throws RuntimeException if Elasticsearch operation fails
     */
    @Transactional(readOnly = true)
    public Map<String, Object> indexJob(Long jobId) {
        // Load job from PostgreSQL
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + jobId));

        try {
            // Create JobDocument from Job entity
            JobDocument document = createJobDocument(job);

            // Save to Elasticsearch
            elasticsearchClient.put()
                    .uri("/jobs/_doc/{id}", document.getId())
                    .body(document)
                    .retrieve()
                    .toBodilessEntity();

            // Create indexing response
            Map<String, Object> indexingResult = createIndexingResponse(jobId, document);
            
            // Notify observers of INDEXED event
            notifyObservers("INDEXED", indexingResult);
            
            return indexingResult;
        } catch (Exception e) {
            // Re-throw for controller to handle as 500 error
            throw new RuntimeException("Failed to index job " + jobId + " in Elasticsearch: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a JobDocument from a Job entity.
     *
     * @param job the job to convert
     * @return the JobDocument representing the job
     */
    private JobDocument createJobDocument(Job job) {
        return new JobDocument(
                job.getId(),
                job.getTitle(),
                job.getDescription(),
                job.getCategory() != null ? job.getCategory().name() : null,
                job.getBudgetMin(),
                job.getBudgetMax(),
                job.getRating(),
                job.getStatus() != null ? job.getStatus().name() : null
        );
    }

    /**
     * Creates the response map for a successful indexing operation.
     *
     * @param jobId the indexed job ID
     * @param document the indexed JobDocument
     * @return response map with indexing details
     */
    private Map<String, Object> createIndexingResponse(Long jobId, JobDocument document) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jobId", jobId);
        response.put("indexed", true);
        response.put("indexedFields", List.of(
                "id",
                "title",
                "description",
                "category",
                "budgetMin",
                "budgetMax",
                "rating",
                "status"
        ));
        response.put("source", "explicit");
        return response;
    }

    private void notifyObservers(String eventType, Map<String, Object> payload) {
        for (EntityObserver observer : observers) {
            try {
                observer.onEvent(eventType, payload);
            } catch (Exception e) {
                System.err.println("Failed to notify observer: " + e.getMessage());
            }
        }
    }

    private String firstElasticsearchUri(String elasticsearchUris) {
        return elasticsearchUris.split(",")[0].trim();
    }
}
