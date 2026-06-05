package com.team01.freelance.job.service;

import com.team01.freelance.job.dto.JobProposalSummaryDTO;
import com.team01.freelance.job.client.ContractLookupClient;
import com.team01.freelance.job.client.ContractSummary;
import com.team01.freelance.job.exception.ForbiddenOperationException;
import com.team01.freelance.job.model.JobAttachmentAlertDTO;
import com.team01.freelance.job.model.JobAttachment;
import com.team01.freelance.job.model.JobAttachmentVerificationRequest;
import com.team01.freelance.job.model.Job;
import com.team01.freelance.job.model.JobRatingRequest;
import com.team01.freelance.job.model.JobStatus;
import com.team01.freelance.job.dto.TopBudgetJobDTO;
import com.team01.freelance.job.repository.JobAttachmentRepository;
import com.team01.freelance.job.repository.JobRepository;
import com.team01.freelance.user.model.User;
import com.team01.freelance.user.model.UserRole;
import com.team01.freelance.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import com.team01.freelance.common.observer.EventSubject;
import com.team01.freelance.job.dto.JobDashboardDTO;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Map;

/**
 * Coordinates job use cases, projections, and event publishing.
 */
@Service
public class JobService {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContractLookupClient contractLookupClient;

    @Autowired
    private JobAttachmentRepository jobAttachmentRepository;

    @Autowired
    private DataSource dataSource;

    @Autowired(required = false)
    private EventSubject jobEventSubject;

    @Autowired(required = false)
    @Lazy
    private JobService self;

    /**
     * Returns all jobs.
     */
    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    /**
     * Returns a job by ID if it exists.
     */
    public Optional<Job> getJobById(Long id) {
        return jobRepository.findById(id);
    }

    /**
     * Searches jobs by status and budget overlap.
     */
    public Page<Job> searchJobsByStatusAndBudgetRange(String status, Double minBudget, Double maxBudget, Pageable pageable) {
        if (minBudget == null || maxBudget == null) {
            throw new IllegalArgumentException("minBudget and maxBudget are required");
        }

        if (minBudget < 0 || maxBudget < 0) {
            throw new IllegalArgumentException("minBudget and maxBudget must be non-negative");
        }

        if (minBudget > maxBudget) {
            throw new IllegalArgumentException("minBudget cannot be greater than maxBudget");
        }

        String normalizedStatus = (status == null || status.trim().isEmpty()) ? null : status.trim();
        return jobRepository.searchJobsByStatusAndBudgetRange(normalizedStatus, minBudget, maxBudget, pageable);
    }

    /**
     * Creates a new job and publishes a creation event.
     */
    @CacheEvict(value = "jobDashboard", allEntries = true)
    public Job createJob(Job job) {
        if (job.getClientId() == null) {
            throw new IllegalArgumentException("Client ID is required to create a Job");
        }

        if (job.getBudgetMin() != null && job.getBudgetMax() != null
                && job.getBudgetMin() > job.getBudgetMax()) {
            throw new IllegalArgumentException("Budget minimum cannot be greater than budget maximum");
        }

        userRepository.findById(job.getClientId())
                .orElseThrow(() -> new EntityNotFoundException("Client not found with id: " + job.getClientId()));

        Job savedJob = jobRepository.save(job);
        publishJobEvent("JOB_CREATED", savedJob.getId(), jobEventDetails(savedJob));
        return savedJob;
    }

    /**
     * Updates an existing job and throws if it does not exist.
     * Validates that budgetMin is not greater than budgetMax.
     *
     * @param id The ID of the job to update
     * @param jobDetails The object containing updated fields
     * @return The updated job
     * @throws IllegalArgumentException if the budget range is invalid
     * @throws EntityNotFoundException if the job is not found
     */
    @CacheEvict(value = "jobDashboard", allEntries = true)
    public Job updateJob(Long id, Job jobDetails) {
        return jobRepository.findById(id).map(existingJob -> {
                if (jobDetails.getTitle() != null) existingJob.setTitle(jobDetails.getTitle());
                if (jobDetails.getDescription() != null) existingJob.setDescription(jobDetails.getDescription());
                if (jobDetails.getCategory() != null) existingJob.setCategory(jobDetails.getCategory());
                if (jobDetails.getStatus() != null) existingJob.setStatus(jobDetails.getStatus());
                if (jobDetails.getBudgetMin() != null) existingJob.setBudgetMin(jobDetails.getBudgetMin());
                if (jobDetails.getBudgetMax() != null) existingJob.setBudgetMax(jobDetails.getBudgetMax());
                if (jobDetails.getRequirements() != null) existingJob.setRequirements(jobDetails.getRequirements());
                if (jobDetails.getCreatedAt() != null) existingJob.setCreatedAt(jobDetails.getCreatedAt());
            if (existingJob.getBudgetMin() != null && existingJob.getBudgetMax() != null
                    && existingJob.getBudgetMin() > existingJob.getBudgetMax()) {
                throw new IllegalArgumentException("Budget minimum cannot be greater than budget maximum");
            }

            Job savedJob = jobRepository.save(existingJob);
            publishJobEvent("JOB_UPDATED", savedJob.getId(), jobEventDetails(savedJob));
            return savedJob;
        }).orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + id));
    }

    /**
     * Replaces the requirements map for an existing job.
     */
    @CacheEvict(value = "jobDashboard", allEntries = true)
    public Job updateJobRequirements(Long id, Map<String, Object> requirements) {
        return jobRepository.findById(id).map(existingJob -> {
            Map<String, Object> mergedRequirements = new HashMap<>();
            if (existingJob.getRequirements() != null) {
                mergedRequirements.putAll(existingJob.getRequirements());
            }
            if (requirements != null) {
                mergedRequirements.putAll(requirements);
            }
            existingJob.setRequirements(mergedRequirements);
            Job savedJob = jobRepository.save(existingJob);
            publishJobEvent("JOB_UPDATED", savedJob.getId(), jobEventDetails(savedJob));
            return savedJob;
        }).orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + id));
    }

    /**
     * Deletes a job when it exists.
     */
    @CacheEvict(value = "jobDashboard", allEntries = true)
    public boolean deleteJobById(Long id) {
        if (!jobRepository.existsById(id)) {
            return false;
        }
        jobRepository.deleteById(id);
        publishJobEvent("JOB_DELETED", id, Map.of());
        return true;
    }

    /**
     * Deletes every job.
     */
    @CacheEvict(value = "jobDashboard", allEntries = true)
    public void deleteAllJobs() {
        jobRepository.deleteAll();
        publishJobEvent("JOB_BULK_DELETED", null, Map.of());
    }

    /**
     * Retrieves proposal summary statistics for a job within a date range.
     * Aggregates total proposals, average bid amount, lowest bid, and highest bid.
     *
     * @param jobId the job ID
     * @param startDate inclusive start date of the range
     * @param endDate inclusive end date of the range
     * @return JobProposalSummaryDTO with aggregated proposal data
     * @throws IllegalArgumentException if startDate is after endDate
     * @throws EntityNotFoundException if job is not found
     */
    public JobProposalSummaryDTO getJobProposalSummary(Long jobId, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("startDate and endDate are required");
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate must be on or before endDate");
        }

        // Verify job exists
        if (!jobRepository.existsById(jobId)) {
            throw new EntityNotFoundException("Job not found with id: " + jobId);
        }

        // Convert LocalDate to LocalDateTime for database query
        // Use half-open interval: [startDate, endDate+1) for inclusive end date
        LocalDateTime queryStart = startDate.atStartOfDay();
        LocalDateTime queryEndExclusive = endDate.plusDays(1).atStartOfDay();

        List<Object[]> rows = jobRepository.getProposalSummary(jobId, queryStart, queryEndExclusive);
        if (rows.isEmpty()) {
            throw new EntityNotFoundException("Job not found with id: " + jobId);
        }
        return toJobProposalSummaryDTO(rows.get(0));
    }

    /**
     * Maps a proposal summary row to its DTO.
     */
    private JobProposalSummaryDTO toJobProposalSummaryDTO(Object[] row) {
        return JobProposalSummaryDTO.builder()
            .jobId(row[0] != null ? ((Number) row[0]).longValue() : null)
            .title(row[1] != null ? row[1].toString() : null)
            .totalProposals(row[2] != null ? ((Number) row[2]).longValue() : 0L)
            .averageBidAmount(row[3] != null ? ((Number) row[3]).doubleValue() : 0.0)
            .lowestBid(row[4] != null ? ((Number) row[4]).doubleValue() : 0.0)
            .highestBid(row[5] != null ? ((Number) row[5]).doubleValue() : 0.0)
            .build();
    }
    /**
     * Returns jobs that still have expired attachments.
     */
    @Transactional(readOnly = true)
    public List<JobAttachmentAlertDTO> getJobsWithExpiredAttachments() {
        LocalDate today = LocalDate.now();

        return jobRepository.findJobsWithExpiredAttachments().stream()
                .map(job -> {
                    List<JobAttachment> expiredAttachments = job.getJobAttachments() == null
                            ? List.of()
                            : job.getJobAttachments().stream()
                                    .filter(attachment -> attachment.getExpiryDate() != null
                                            && attachment.getExpiryDate().isBefore(today))
                                    .toList();

                    if (expiredAttachments.isEmpty()) {
                        return null;
                    }

                    JobAttachmentAlertDTO dto = new JobAttachmentAlertDTO();
                    dto.setJobId(job.getId());
                    dto.setJobTitle(job.getTitle());
                    dto.setJobStatus(job.getStatus());
                    dto.setExpiredAttachments(expiredAttachments);
                    dto.setExpiredCount(expiredAttachments.size());
                    return dto;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Rates a job using a completed contract.
     */
    @Transactional
    @CacheEvict(value = "jobDashboard", allEntries = true)
    public Job rateJob(Long jobId, JobRatingRequest ratingRequest) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + jobId));

        if (ratingRequest == null || ratingRequest.getContractId() == null || ratingRequest.getRating() == null) {
            throw new IllegalArgumentException("Contract ID and rating are required to rate a Job");
        }

        if (ratingRequest.getRating() < 1 || ratingRequest.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        ContractSummary contract = contractLookupClient.getContractById(ratingRequest.getContractId());

        if (contract.getJobId() == null || !jobId.equals(contract.getJobId())) {
            throw new IllegalArgumentException("Contract must reference the job being rated");
        }

        if (contract.getStatus() == null || !"COMPLETED".equalsIgnoreCase(contract.getStatus())) {
            throw new IllegalArgumentException("Contract must be completed before rating a Job");
        }

        double currentRating = job.getRating() == null ? 0.0 : job.getRating();
        int currentTotalRatings = job.getTotalRatings() == null ? 0 : job.getTotalRatings();

        double newRating = ((currentRating * currentTotalRatings) + ratingRequest.getRating()) / (currentTotalRatings + 1);
        job.setRating(newRating);
        job.setTotalRatings(currentTotalRatings + 1);

        Job savedJob = jobRepository.save(job);
        publishJobEvent("JOB_RATED", savedJob.getId(), Map.of(
            "contractId", ratingRequest.getContractId(),
            "rating", ratingRequest.getRating(),
            "ratingCount", savedJob.getTotalRatings()
        ));
        return savedJob;
    }

    /**
     * Verifies a job attachment and records the verifier metadata.
     */
    @Transactional
    @CacheEvict(value = "jobDashboard", allEntries = true)
    public Job verifyJobAttachment(Long jobId, Long attachmentId, JobAttachmentVerificationRequest request) {
        if (request == null || request.getVerifiedBy() == null) {
            throw new IllegalArgumentException("verifiedBy is required to verify a JobAttachment");
        }

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + jobId));

        JobAttachment attachment = jobAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new EntityNotFoundException("Job Attachment not found with id: " + attachmentId));

        if (attachment.getJob() == null || !jobId.equals(attachment.getJob().getId())) {
            throw new IllegalArgumentException("Job attachment does not belong to the specified job");
        }

        LocalDate expiryDate = attachment.getExpiryDate();
        if (expiryDate == null || !expiryDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Job attachment has expired");
        }

        User verifier = userRepository.findById(request.getVerifiedBy())
                .orElseThrow(() -> new ForbiddenOperationException("VerifiedBy user must be an admin user"));

        if (verifier.getRole() != UserRole.ADMIN) {
            throw new ForbiddenOperationException("VerifiedBy user must be an admin user");
        }

        attachment.setVerified(true);

        Map<String, Object> metadata = attachment.getMetadata() == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(attachment.getMetadata());
        metadata.put("verifiedAt", LocalDateTime.now().toString());
        metadata.put("verifiedBy", request.getVerifiedBy());
        attachment.setMetadata(metadata);

        jobAttachmentRepository.save(attachment);

        publishJobEvent("JOB_ATTACHMENT_VERIFIED", jobId, Map.of(
            "attachmentId", attachmentId,
            "verifiedBy", request.getVerifiedBy(),
            "verified", true
        ));

        Job refreshedJob = jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + jobId));

        if (refreshedJob.getJobAttachments() != null) {
            refreshedJob.getJobAttachments().size();
        }

        return refreshedJob;
    }

    /**
     * Searches jobs by a key-value pair in the requirements JSONB column.
     * Optionally filters by job status.
     *
     * @param key the JSON key to search for in requirements
     * @param value the value to match
     * @param status the optional job status filter
     * @return a list of jobs matching the criteria
     */
    public List<Job> searchByRequirements(String key, String value, String status) {
        if (!usesPostgresDatabase()) {
            return filterJobsByRequirements(key, value, status);
        }
        try {
            if (status != null && !status.isEmpty()) {
                return jobRepository.searchByRequirements(key, value, status);
            }
            return jobRepository.searchByRequirements(key, value);
        } catch (DataAccessException ex) {
            return filterJobsByRequirements(key, value, status);
        }
    }

    /**
     * Filters jobs by requirements in memory.
     */
    private List<Job> filterJobsByRequirements(String key, String value, String status) {
        return jobRepository.findAll().stream()
                .filter(job -> job.getRequirements() != null
                        && value.equals(String.valueOf(job.getRequirements().get(key))))
                .filter(job -> status == null || status.isEmpty()
                        || (job.getStatus() != null && status.equalsIgnoreCase(job.getStatus().name())))
                .toList();
    }

    /**
     * Detects whether the current datasource is PostgreSQL.
     */
    private boolean usesPostgresDatabase() {
        try (var connection = dataSource.getConnection()) {
            return "PostgreSQL".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName());
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Closes a job and rejects all related SUBMITTED proposals.
     * Eligibility and status transition are enforced in one conditional UPDATE so a
     * concurrent contract activation cannot slip in between check and write.
     *
     * @param jobId the job ID to close
     * @return the closed job
     * @throws EntityNotFoundException if the job is not found
     * @throws IllegalArgumentException if an ACTIVE contract exists for the job
     */
    @Transactional
    @CacheEvict(value = "jobDashboard", allEntries = true)
    public Job closeJob(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + jobId));

        if (job.getStatus() == JobStatus.CLOSED) {
            return job;
        }

        int updated = jobRepository.closeJobIfEligible(jobId);
        if (updated == 0) {
            Job current = jobRepository.findById(jobId).orElseThrow();
            if (current.getStatus() == JobStatus.CLOSED) {
                return current;
            }
            throw new IllegalArgumentException("Cannot close job with an active contract");
        }

        jobRepository.rejectSubmittedProposalsByJobId(jobId);

        return jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + jobId));
    }

    /**
     * Returns the jobs with the highest budgets.
     */
    public List<TopBudgetJobDTO> getTopBudgetJobs(int limit) {
        return jobRepository.findTopBudgetJobs(limit);
    }

    /**
     * Returns the dashboard view and records the access event.
     */
    public List<JobDashboardDTO> getJobDashboard() {
        publishJobEvent("DASHBOARD_VIEWED", null, Map.of("view", "dashboard"));
        return self == null ? loadJobDashboard() : self.loadJobDashboard();
    }

    /**
     * Loads the job dashboard projection from the repository.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "jobDashboard", key = "'all'")
    public List<JobDashboardDTO> loadJobDashboard() {
        List<Object[]> rows = jobRepository.findJobDashboard();
        return rows.stream().map(r -> JobDashboardDTO.builder()
                .jobId(r[0] != null ? ((Number) r[0]).longValue() : null)
                .title(r[1] != null ? r[1].toString() : null)
                .totalProposals(r[2] != null ? ((Number) r[2]).longValue() : 0L)
                .averageBidAmount(r[3] != null ? ((Number) r[3]).doubleValue() : 0.0)
                .activeAttachments(r[4] != null ? ((Number) r[4]).longValue() : 0L)
                .rating(r[5] != null ? ((Number) r[5]).doubleValue() : 0.0)
                .build())
                .toList();
    }

    /**
     * Publishes a job event when event logging is enabled.
     */
    private void publishJobEvent(String eventType, Long jobId, Map<String, Object> details) {
        if (jobEventSubject == null) {
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("jobId", jobId);
        payload.put("details", details == null ? Map.of() : details);
        payload.put("timestamp", LocalDateTime.now());
        payload.put("action", eventType);
        jobEventSubject.notifyObservers(eventType, payload);
    }

    /**
     * Builds a compact event payload from the job state.
     */
    private Map<String, Object> jobEventDetails(Job job) {
        Map<String, Object> details = new HashMap<>();
        if (job.getTitle() != null) {
            details.put("title", job.getTitle());
        }
        if (job.getStatus() != null) {
            details.put("status", job.getStatus().name());
        }
        return details;
    }
}
