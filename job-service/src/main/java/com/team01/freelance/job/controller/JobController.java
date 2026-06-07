package com.team01.freelance.job.controller;

import com.team01.freelance.job.dto.JobDashboardDTO;
import com.team01.freelance.job.dto.JobProposalSummaryDTO;
import com.team01.freelance.job.exception.ForbiddenOperationException;
import com.team01.freelance.job.model.JobAttachmentAlertDTO;
import com.team01.freelance.job.model.JobAttachmentVerificationRequest;
import com.team01.freelance.job.model.JobCloseRequest;
import com.team01.freelance.job.service.JobIndexService;
import com.team01.freelance.job.model.Job;
import com.team01.freelance.job.model.JobCategory;
import com.team01.freelance.job.model.JobStatus;
import com.team01.freelance.job.search.dto.JobSearchResultDTO;
import com.team01.freelance.job.search.service.JobFullTextSearchOperations;
import com.team01.freelance.job.dto.TopBudgetJobDTO;
import com.team01.freelance.job.service.JobService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;
    private final JobIndexService jobIndexService;

    public JobController(JobService jobService, JobIndexService jobIndexService) {
        this.jobService = jobService;
        this.jobIndexService = jobIndexService;
    }

    @Autowired
    private JobFullTextSearchOperations jobFullTextSearchOperations;

    @GetMapping
    public ResponseEntity<List<Job>> getAllJobs() {
        return ResponseEntity.ok(jobService.getAllJobs());
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchJobs(
            @RequestParam(required = false) String status,
            @RequestParam(required = true) Double minBudget,
            @RequestParam(required = true) Double maxBudget,
            Pageable pageable) {
        try {
            Page<Job> results = jobService.searchJobsByStatusAndBudgetRange(status, minBudget, maxBudget, pageable);
            return ResponseEntity.ok(results);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * [S2-F10] Searches jobs in Elasticsearch by title and description with optional filters.
     *
     * @param query required free-text search terms
     * @param category optional category filter
     * @param status optional status filter
     * @param minBudget optional minimum budget filter
     * @param maxBudget optional maximum budget filter
     * @param pageable pagination settings
     * @return 200 with relevance-ranked results, or 400 when budget bounds are invalid
     */
    @GetMapping("/search/full-text")
    public ResponseEntity<?> fullTextSearchJobs(
            @RequestParam String query,
            @RequestParam(required = false) JobCategory category,
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) Double minBudget,
            @RequestParam(required = false) Double maxBudget,
            Pageable pageable) {
        try {
            Page<JobSearchResultDTO> results = jobFullTextSearchOperations.search(
                    query, category, status, minBudget, maxBudget, pageable);
            return ResponseEntity.ok(results);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}/dashboard")
    public ResponseEntity<JobDashboardDTO> getJobDashboard(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(jobService.getJobDashboard(id));
        } catch (jakarta.persistence.EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Job> getJobById(@PathVariable Long id) {
        return jobService.getJobById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * [S2-F3] Retrieves job proposal summary statistics within a date range.
     *
     * @param id the job ID
     * @param startDate inclusive start date (ISO 8601 format: YYYY-MM-DD)
     * @param endDate inclusive end date (ISO 8601 format: YYYY-MM-DD)
     * @return 200 with JobProposalSummaryDTO, 400 for invalid date range, or 404 if job not found
     */
    @GetMapping("/{id}/proposal-summary")
    public ResponseEntity<JobProposalSummaryDTO> getJobProposalSummary(
            @PathVariable Long id,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        return ResponseEntity.ok(jobService.getJobProposalSummary(id, startDate, endDate));
    }

    @PostMapping
    public ResponseEntity<Job> createJob(@RequestBody Job job) {
        return ResponseEntity.ok(jobService.createJob(job));
    }

    /**
     * Updates a job by ID.
     *
     * @param id the job ID
     * @param job the update payload
     * @return 200 with updated job, or 404 if not found
     */
    @PutMapping("/{id}")
    public ResponseEntity<Job> updateJob(@PathVariable Long id, @RequestBody Job job) {
        try {
            return ResponseEntity.ok(jobService.updateJob(id, job));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/requirements")
    public ResponseEntity<Job> updateJobRequirements(@PathVariable Long id,
                                                     @RequestBody Map<String, Object> requirements) {
        try {
            return ResponseEntity.ok(jobService.updateJobRequirements(id, requirements));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJobById(@PathVariable Long id) {
        if (jobService.deleteJobById(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/all")
    public ResponseEntity<Void> deleteAllJobs() {
        jobService.deleteAllJobs();
        return ResponseEntity.noContent().build();
    }

    /**
     * Returns a report of jobs that have expired attachments.
     *
     * @return 200 with job attachment alert DTOs
     */
    @GetMapping("/attachments/expired")
    public ResponseEntity<List<JobAttachmentAlertDTO>> getJobsWithExpiredAttachments() {
        return ResponseEntity.ok(jobService.getJobsWithExpiredAttachments());
    }

    /**
     * Rates a job using a completed contract that references the job.
     *
     * @param id the job ID
     * @param ratingRequest the rating payload
     * @return 200 with updated job, 400 for invalid contract or rating, or 404 if job or contract is not found
     */
    @PostMapping("/{id}/rate")
    public ResponseEntity<Job> rateJob(
            @PathVariable Long id,
            @RequestBody com.team01.freelance.job.model.JobRatingRequest ratingRequest) {
        try {
            return ResponseEntity.ok(jobService.rateJob(id, ratingRequest));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * [S2-F11] Indexes a job in Elasticsearch for full-text search.
     * Loads the job from PostgreSQL, creates/updates an Elasticsearch document,
     * and logs an INDEXED event through the Observer chain.
     *
     * @param id the job ID to index
     * @return 200 with indexing status, 404 if job not found, or 500 if Elasticsearch fails
     */
    @PostMapping("/{id}/index")
    public ResponseEntity<?> indexJob(@PathVariable Long id) {
        try {
            Map<String, Object> indexingResult = jobIndexService.indexJob(id);
            return ResponseEntity.ok(indexingResult);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new LinkedHashMap<>();
            errorResponse.put("error", "Elasticsearch indexing failed");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Verifies a job attachment for a specific job.
     *
     * @param jobId the job ID
     * @param attachmentId the attachment ID
     * @param request the verification payload
     * @return 200 with updated job, 400 for invalid attachment or expiry, 403 if verifier is not an admin, or 404 if job or attachment is not found
     */
    @PutMapping("/{jobId}/attachments/{attachmentId}/verify")
    public ResponseEntity<Map<String, Object>> verifyJobAttachment(
            @PathVariable Long jobId,
            @PathVariable Long attachmentId,
            @RequestBody JobAttachmentVerificationRequest request) {
        try {
            return ResponseEntity.ok(toJobWithAttachmentsResponse(jobService.verifyJobAttachment(jobId, attachmentId, request)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (ForbiddenOperationException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

   
    /**
     * Searches jobs by a key-value pair in the requirements JSONB column.
     * Optionally filters by job status.
     *
     * @param key the JSON key to search for in requirements
     * @param value the value to match
     * @param status the optional job status filter
     * @return 200 with list of matching jobs
     */
    /**
     * Closes a job and rejects all related SUBMITTED proposals.
     *
     * @param id the job ID
     * @param closeRequest must contain {@code status: "CLOSED"}
     * @return 200 with closed job, 400 if an ACTIVE contract exists or status is invalid, or 404 if not found
     */
    @PutMapping("/{id}/close")
    public ResponseEntity<Job> closeJob(@PathVariable Long id, @RequestBody JobCloseRequest closeRequest) {
        if (closeRequest == null || closeRequest.getStatus() == null
                || !JobStatus.CLOSED.name().equalsIgnoreCase(closeRequest.getStatus())) {
            return ResponseEntity.badRequest().build();
        }
        try {
            return ResponseEntity.ok(jobService.closeJob(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/requirements/search")
    public ResponseEntity<List<Job>> searchByRequirements(
            @RequestParam String key,
            @RequestParam String value,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(jobService.searchByRequirements(key, value, status));
    }

    private Map<String, Object> toJobWithAttachmentsResponse(Job job) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", job.getId());
        response.put("clientId", job.getClientId());
        response.put("title", job.getTitle());
        response.put("description", job.getDescription());
        response.put("category", job.getCategory());
        response.put("status", job.getStatus());
        response.put("budgetMin", job.getBudgetMin());
        response.put("budgetMax", job.getBudgetMax());
        response.put("rating", job.getRating());
        response.put("totalRatings", job.getTotalRatings());
        response.put("requirements", job.getRequirements());
        response.put("createdAt", job.getCreatedAt());
        response.put("jobAttachments", job.getJobAttachments() == null ? List.of() : job.getJobAttachments());
        return response;
    }

    /**
     * Retrieves the top jobs ordered by budgetMax in descending order.
     * Includes the count of proposals for each job.
     *
     * @param limit the maximum number of jobs to return (default: 10)
     * @return 200 with list of top budget jobs
     */
    /**
     * Retrieves the top jobs ordered by budgetMax in descending order.
     * Includes the count of proposals for each job.
     *
     * @param limit the maximum number of jobs to return (default: 10)
     * @return 200 with list of top budget jobs
     */
    @GetMapping("/reports/top-budget")
    public ResponseEntity<List<TopBudgetJobDTO>> getTopBudgetJobs(
            @RequestParam(name = "limit", defaultValue = "10") int limit) {
        return ResponseEntity.ok(jobService.getTopBudgetJobs(limit));
    }
}
