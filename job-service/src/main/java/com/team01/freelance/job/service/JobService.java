package com.team01.freelance.job.service;

import com.team01.freelance.common.observer.EventSubject;
import com.team01.freelance.contracts.events.ProposalAcceptedEvent;
import com.team01.freelance.contracts.events.ProposalCancelledEvent;
import com.team01.freelance.contracts.events.ProposalCompletedEvent;
import com.team01.freelance.contracts.events.ProposalWithdrawnEvent;
import com.team01.freelance.job.dto.JobDashboardDTO;
import com.team01.freelance.job.dto.JobProposalSummaryDTO;
import com.team01.freelance.job.feign.ContractServiceClient;
import com.team01.freelance.job.feign.ProposalServiceClient;
import com.team01.freelance.job.feign.dto.ContractResponse;
import com.team01.freelance.job.feign.dto.ProposalJobSummaryByJobResponse;
import com.team01.freelance.job.feign.dto.ProposalJobSummaryResponse;
import com.team01.freelance.job.event.JobEventTypes;
import com.team01.freelance.job.exception.ForbiddenOperationException;
import com.team01.freelance.job.messaging.publishers.JobEventPublisher;
import com.team01.freelance.job.model.JobAttachmentAlertDTO;
import com.team01.freelance.job.model.JobAttachment;
import com.team01.freelance.job.model.JobAttachmentVerificationRequest;
import com.team01.freelance.job.model.Job;
import com.team01.freelance.job.model.JobRatingRequest;
import com.team01.freelance.job.model.JobStatus;
import com.team01.freelance.job.dto.TopBudgetJobDTO;
import com.team01.freelance.job.repository.JobAttachmentRepository;
import com.team01.freelance.job.repository.JobRepository;
import com.team01.freelance.job.search.service.JobSearchIndexOperations;
import com.team01.freelance.job.feign.UserServiceClient;
import com.team01.freelance.job.feign.dto.UserDTO;
import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Service
public class JobService {

    private static final Logger log = LoggerFactory.getLogger(JobService.class);
    private static final long SLOW_DASHBOARD_THRESHOLD_MS = 1000L;
    private static final int MAX_TOP_BUDGET_LIMIT = 50;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserServiceClient userServiceClient;

    @Autowired
    private ContractServiceClient contractServiceClient;

    @Autowired
    private ProposalServiceClient proposalServiceClient;

    @Autowired
    private JobEventPublisher jobEventPublisher;

    @Autowired
    private JobAttachmentRepository jobAttachmentRepository;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JobSearchIndexOperations jobSearchIndexOperations;

    @Autowired
    @Qualifier("jobEventSubject")
    private EventSubject jobEventSubject;

    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    public Optional<Job> getJobById(Long id) {
        return jobRepository.findById(id);
    }

    @Cacheable(
            value = "S2-F1",
            key = "(#status == null ? 'ALL' : #status.trim()) + ':' + #minBudget + ':' + #maxBudget + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort.toString()"
    )
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

    @Caching(evict = {
            @CacheEvict(value = "S2-F1", allEntries = true),
            @CacheEvict(value = "S2-F5", allEntries = true),
            @CacheEvict(value = "S2-F6", allEntries = true)
    })
    public Job createJob(Job job) {
        if (job.getClientId() == null) {
            throw new IllegalArgumentException("Client ID is required to create a Job");
        }

        if (job.getBudgetMin() != null && job.getBudgetMax() != null
                && job.getBudgetMin() > job.getBudgetMax()) {
            throw new IllegalArgumentException("Budget minimum cannot be greater than budget maximum");
        }

        try {
            userServiceClient.getUser(job.getClientId());
        } catch (FeignException.NotFound e) {
            throw new EntityNotFoundException("Client not found with id: " + job.getClientId());
        }

        Job savedJob = jobRepository.save(job);
        jobSearchIndexOperations.index(savedJob);
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
    @Caching(evict = {
            @CacheEvict(value = "job-by-id", key = "#id"),
            @CacheEvict(value = "S2-F1", allEntries = true),
            @CacheEvict(value = "S2-F3", allEntries = true),
            @CacheEvict(value = "S2-F5", allEntries = true),
            @CacheEvict(value = "S2-F6", allEntries = true),
            @CacheEvict(value = "S2-F12", key = "#id")
    })
    public Job updateJob(Long id, Job jobDetails) {
        return jobRepository.findById(id).map(existingJob -> {
                JobStatus previousStatus = existingJob.getStatus();
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
            jobSearchIndexOperations.index(savedJob);
            publishJobEvent("JOB_UPDATED", savedJob.getId(), jobEventDetails(savedJob));
            if (jobDetails.getStatus() != null && previousStatus != savedJob.getStatus()) {
                jobEventPublisher.publishJobStatusChanged(savedJob.getId(), previousStatus, savedJob.getStatus());
            }
            return savedJob;
        }).orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + id));
    }

    @Caching(evict = {
            @CacheEvict(value = "job-by-id", key = "#id"),
            @CacheEvict(value = "S2-F5", allEntries = true)
    })
    public Job updateJobRequirements(Long id, Map<String, Object> requirements) {
        return jobRepository.findById(id).map(existingJob -> {
            Map<String, Object> changedRequirements = requirements == null
                    ? Map.of()
                    : new LinkedHashMap<>(requirements);
            Map<String, Object> mergedRequirements = new HashMap<>();
            if (existingJob.getRequirements() != null) {
                mergedRequirements.putAll(existingJob.getRequirements());
            }
            mergedRequirements.putAll(changedRequirements);
            existingJob.setRequirements(mergedRequirements);
            Job savedJob = jobRepository.save(existingJob);
            jobSearchIndexOperations.index(savedJob);
            jobEventSubject.notifyObservers(JobEventTypes.REQUIREMENTS_UPDATED_JOB, Map.of(
                    "jobId", savedJob.getId(),
                    "timestamp", LocalDateTime.now(),
                    "changedRequirements", changedRequirements,
                    "requirements", new LinkedHashMap<>(mergedRequirements)
            ));
            return savedJob;
        }).orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + id));
    }

    @Caching(evict = {
            @CacheEvict(value = "job-by-id", key = "#id", condition = "#result == true"),
            @CacheEvict(value = "S2-F1", allEntries = true, condition = "#result == true"),
            @CacheEvict(value = "S2-F3", allEntries = true, condition = "#result == true"),
            @CacheEvict(value = "S2-F5", allEntries = true, condition = "#result == true"),
            @CacheEvict(value = "S2-F6", allEntries = true, condition = "#result == true"),
            @CacheEvict(value = "S2-F12", key = "#id", condition = "#result == true")
    })
    public boolean deleteJobById(Long id) {
        if (!jobRepository.existsById(id)) {
            return false;
        }
        jobRepository.deleteById(id);
        jobSearchIndexOperations.delete(id);
        publishJobEvent("JOB_DELETED", id, Map.of());
        return true;
    }

    @Caching(evict = {
            @CacheEvict(value = "job-by-id", allEntries = true),
            @CacheEvict(value = "S2-F1", allEntries = true),
            @CacheEvict(value = "S2-F3", allEntries = true),
            @CacheEvict(value = "S2-F5", allEntries = true),
            @CacheEvict(value = "S2-F6", allEntries = true),
            @CacheEvict(value = "S2-F12", allEntries = true)
    })
    public void deleteAllJobs() {
        jobRepository.deleteAll();
        jobSearchIndexOperations.deleteAll();
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

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + jobId));

        ProposalJobSummaryResponse summary = fetchProposalSummary(jobId, startDate, endDate);
        return JobProposalSummaryDTO.builder()
                .jobId(jobId)
                .title(job.getTitle())
                .totalProposals(summary.getTotalProposals() != null ? summary.getTotalProposals() : 0L)
                .averageBidAmount(summary.getAverageBidAmount() != null ? summary.getAverageBidAmount() : 0.0)
                .lowestBid(summary.getLowestBid() != null ? summary.getLowestBid() : 0.0)
                .highestBid(summary.getHighestBid() != null ? summary.getHighestBid() : 0.0)
                .build();
    }

    private ProposalJobSummaryResponse fetchProposalSummary(Long jobId, LocalDate startDate, LocalDate endDate) {
        try {
            ProposalJobSummaryResponse summary = proposalServiceClient.getJobProposalSummary(jobId, startDate, endDate);
            if (summary == null) {
                throw new IllegalStateException("Proposal service returned an empty response for job: " + jobId);
            }
            return summary;
        } catch (FeignException.NotFound e) {
            throw new EntityNotFoundException("Job not found with id: " + jobId);
        } catch (FeignException e) {
            throw new IllegalStateException("Failed to fetch proposal summary for job: " + jobId, e);
        }
    }

    private ProposalJobSummaryResponse fetchAllTimeProposalSummary(Long jobId) {
        try {
            ProposalJobSummaryResponse summary = proposalServiceClient.getJobProposalSummary(jobId, null, null);
            if (summary == null) {
                throw new IllegalStateException("Proposal service returned an empty response for job: " + jobId);
            }
            return summary;
        } catch (FeignException e) {
            throw new IllegalStateException("Failed to fetch proposal summary for job: " + jobId, e);
        }
    }

    private Map<Long, ProposalJobSummaryResponse> fetchAllTimeProposalSummaries(List<Long> jobIds) {
        if (jobIds == null || jobIds.isEmpty()) {
            return Map.of();
        }
        try {
            List<ProposalJobSummaryByJobResponse> summaries = proposalServiceClient.getJobProposalSummaries(jobIds);
            if (summaries == null || summaries.isEmpty()) {
                return Map.of();
            }
            Map<Long, ProposalJobSummaryResponse> summariesByJobId = new HashMap<>();
            for (ProposalJobSummaryByJobResponse summary : summaries) {
                if (summary == null || summary.getJobId() == null) {
                    continue;
                }
                summariesByJobId.put(summary.getJobId(), toProposalJobSummaryResponse(summary));
            }
            return summariesByJobId;
        } catch (FeignException e) {
            throw new IllegalStateException("Failed to fetch proposal summaries for jobs: " + jobIds, e);
        }
    }

    private ProposalJobSummaryResponse toProposalJobSummaryResponse(ProposalJobSummaryByJobResponse summary) {
        ProposalJobSummaryResponse response = new ProposalJobSummaryResponse();
        response.setTotalProposals(summary.getTotalProposals());
        response.setAcceptedProposals(summary.getAcceptedProposals());
        response.setAverageBidAmount(summary.getAverageBidAmount());
        response.setLowestBid(summary.getLowestBid());
        response.setHighestBid(summary.getHighestBid());
        return response;
    }

    private int fetchActiveContractCount(Long jobId) {
        try {
            Integer count = contractServiceClient.getActiveContractCountForJob(jobId);
            return count != null ? count : 0;
        } catch (FeignException.BadRequest e) {
            throw new IllegalArgumentException("Invalid job id for active contract lookup: " + jobId);
        } catch (FeignException e) {
            throw new IllegalStateException("Failed to fetch active contract count for job: " + jobId, e);
        }
    }

    private ContractResponse fetchContract(Long contractId) {
        try {
            ContractResponse contract = contractServiceClient.getContractById(contractId);
            if (contract == null) {
                throw new IllegalStateException("Contract service returned an empty response for id: " + contractId);
            }
            return contract;
        } catch (FeignException.NotFound e) {
            throw new EntityNotFoundException("Contract not found with id: " + contractId);
        } catch (FeignException e) {
            throw new IllegalStateException("Failed to fetch contract: " + contractId, e);
        }
    }

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

                        return JobAttachmentAlertDTO.builder()
                            .jobId(job.getId())
                            .jobTitle(job.getTitle())
                            .jobStatus(job.getStatus())
                            .expiredAttachments(expiredAttachments)
                            .expiredCount(expiredAttachments.size())
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "job-by-id", key = "#jobId"),
            @CacheEvict(value = "S2-F1", allEntries = true),
            @CacheEvict(value = "S2-F6", allEntries = true),
            @CacheEvict(value = "S2-F12", key = "#jobId")
    })
    public Job rateJob(Long jobId, JobRatingRequest ratingRequest) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + jobId));

        if (ratingRequest == null || ratingRequest.getContractId() == null || ratingRequest.getRating() == null) {
            throw new IllegalArgumentException("Contract ID and rating are required to rate a Job");
        }

        if (ratingRequest.getRating() < 1 || ratingRequest.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        ContractResponse contract = fetchContract(ratingRequest.getContractId());

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
        jobSearchIndexOperations.index(savedJob);
        publishJobEvent("JOB_RATED", savedJob.getId(), Map.of(
            "contractId", ratingRequest.getContractId(),
            "rating", ratingRequest.getRating(),
            "ratingCount", savedJob.getTotalRatings()
        ));
        publishAfterCommit(() -> jobEventPublisher.publishJobRated(
                savedJob.getId(),
                ratingRequest.getContractId(),
                ratingRequest.getRating(),
                contract.getClientId()
        ));
        return savedJob;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "job-by-id", key = "#jobId"),
            @CacheEvict(value = "job-attachment-by-id", key = "#attachmentId"),
            @CacheEvict(value = "S2-F12", key = "#jobId")
    })
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

        UserDTO verifier;
        try {
            verifier = userServiceClient.getUser(request.getVerifiedBy());
        } catch (FeignException.NotFound e) {
            throw new ForbiddenOperationException("VerifiedBy user must be an admin user");
        }

        if (verifier == null || !"ADMIN".equals(verifier.getRole())) {
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
    @Cacheable(
            value = "S2-F5",
            key = "#key + ':' + #value + ':' + (#status == null ? 'ALL' : #status)",
            unless = "#result == null or #result.isEmpty()"
    )
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

    private List<Job> filterJobsByRequirements(String key, String value, String status) {
        return jobRepository.findAll().stream()
                .filter(job -> job.getRequirements() != null
                        && value.equals(String.valueOf(job.getRequirements().get(key))))
                .filter(job -> status == null || status.isEmpty()
                        || (job.getStatus() != null && status.equalsIgnoreCase(job.getStatus().name())))
                .toList();
    }

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
    @Caching(evict = {
            @CacheEvict(value = "job-by-id", key = "#jobId"),
            @CacheEvict(value = "S2-F1", allEntries = true),
            @CacheEvict(value = "S2-F5", allEntries = true),
            @CacheEvict(value = "S2-F6", allEntries = true),
            @CacheEvict(value = "S2-F12", key = "#jobId")
    })
    public Job closeJob(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + jobId));

        if (job.getStatus() == JobStatus.CLOSED) {
            return job;
        }

        if (fetchActiveContractCount(jobId) > 0) {
            throw new IllegalArgumentException("Cannot close job with an active contract");
        }

        JobStatus previousStatus = job.getStatus();
        job.setStatus(JobStatus.CLOSED);
        Job closedJob = jobRepository.save(job);
        jobSearchIndexOperations.index(closedJob);
        publishJobClosedEvent(closedJob);
        jobEventPublisher.publishJobClosed(closedJob);
        jobEventPublisher.publishJobStatusChanged(closedJob.getId(), previousStatus, JobStatus.CLOSED);
        return closedJob;
    }

    private void publishJobClosedEvent(Job job) {
        jobEventSubject.notifyObservers("JOB_CLOSED", Map.of(
                "jobId", job.getId(),
                "status", job.getStatus().name(),
                "source", "S2-F4"
        ));
    }

    public JobDashboardDTO getJobDashboard(Long id) {
        if (!jobRepository.existsById(id)) {
            throw new EntityNotFoundException("Job not found with id: " + id);
        }
        jobEventSubject.notifyObservers(JobEventTypes.DASHBOARD_VIEWED, Map.of(
                "jobId", id,
                "timestamp", LocalDateTime.now()
        ));
        return getCachedJobDashboard(id);
    }

    @Cacheable(value = "S2-F12", key = "#id")
    public JobDashboardDTO getCachedJobDashboard(Long id) {
        long startedAt = System.currentTimeMillis();
        List<Object[]> rows = jobRepository.getJobDashboardBase(id);
        if (rows == null || rows.isEmpty()) {
            throw new EntityNotFoundException("Job not found with id: " + id);
        }
        ProposalJobSummaryResponse summary = fetchAllTimeProposalSummary(id);
        Object[] row = rows.get(0);
        JobDashboardDTO dashboard = JobDashboardDTO.builder()
                .jobId(row[0] != null ? ((Number) row[0]).longValue() : null)
                .title(row[1] != null ? row[1].toString() : null)
                .rating(row[2] != null ? ((Number) row[2]).doubleValue() : 0.0)
                .totalProposals(summary.getTotalProposals() != null ? summary.getTotalProposals() : 0L)
                .acceptedProposals(summary.getAcceptedProposals() != null ? summary.getAcceptedProposals() : 0L)
                .averageBidAmount(summary.getAverageBidAmount() != null ? summary.getAverageBidAmount() : 0.0)
                .activeAttachments(row[3] != null ? ((Number) row[3]).longValue() : 0L)
                .build();
        long elapsedMs = System.currentTimeMillis() - startedAt;
        if (elapsedMs > SLOW_DASHBOARD_THRESHOLD_MS) {
            log.warn("Slow S2-F12 dashboard aggregation took {}ms", elapsedMs);
        }
        return dashboard;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "job-by-id", key = "#event.jobId()"),
            @CacheEvict(value = "S2-F1", allEntries = true),
            @CacheEvict(value = "S2-F3", allEntries = true),
            @CacheEvict(value = "S2-F12", key = "#event.jobId()")
    })
    public void handleProposalAccepted(ProposalAcceptedEvent event) {
        Job job = jobRepository.findById(event.jobId())
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + event.jobId()));
        JobStatus previousStatus = job.getStatus();
        jobRepository.markJobInProgress(event.jobId());
        jobEventPublisher.publishJobStatusChanged(event.jobId(), previousStatus, JobStatus.IN_PROGRESS);
        jobSearchIndexOperations.index(jobRepository.findById(event.jobId()).orElse(job));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "job-by-id", key = "#event.jobId()"),
            @CacheEvict(value = "S2-F1", allEntries = true),
            @CacheEvict(value = "S2-F12", key = "#event.jobId()")
    })
    public void handleProposalCompleted(ProposalCompletedEvent event) {
        Job job = jobRepository.findById(event.jobId())
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + event.jobId()));
        JobStatus previousStatus = job.getStatus();
        jobRepository.markJobClosed(event.jobId());
        jobEventPublisher.publishJobStatusChanged(event.jobId(), previousStatus, JobStatus.CLOSED);
        jobSearchIndexOperations.index(jobRepository.findById(event.jobId()).orElse(job));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "job-by-id", key = "#event.jobId()"),
            @CacheEvict(value = "S2-F1", allEntries = true),
            @CacheEvict(value = "S2-F12", key = "#event.jobId()")
    })
    public void handleProposalCancelled(ProposalCancelledEvent event) {
        Job job = jobRepository.findById(event.jobId())
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + event.jobId()));
        if (job.getStatus() != JobStatus.IN_PROGRESS) {
            return;
        }
        int updated = jobRepository.reopenIfInProgress(event.jobId());
        if (updated > 0) {
            jobEventPublisher.publishJobStatusChanged(event.jobId(), JobStatus.IN_PROGRESS, JobStatus.OPEN);
            jobSearchIndexOperations.index(jobRepository.findById(event.jobId()).orElse(job));
        }
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "job-by-id", key = "#event.jobId()"),
            @CacheEvict(value = "S2-F1", allEntries = true),
            @CacheEvict(value = "S2-F12", key = "#event.jobId()")
    })
    public void handleProposalWithdrawn(ProposalWithdrawnEvent event) {
        Job job = jobRepository.findById(event.jobId())
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + event.jobId()));
        Integer activeCount = proposalServiceClient.getActiveProposalCountForJob(event.jobId());
        if (activeCount != null && activeCount == 0) {
            int updated = jobRepository.reopenIfInProgress(event.jobId());
            if (updated > 0) {
                jobEventPublisher.publishJobStatusChanged(event.jobId(), JobStatus.IN_PROGRESS, JobStatus.OPEN);
                jobSearchIndexOperations.index(jobRepository.findById(event.jobId()).orElse(job));
            }
        }
    }

    /**
     * Retrieves the top jobs ordered by budgetMax in descending order.
     * Includes the count of proposals for each job.
     *
     * @param limit the maximum number of jobs to return
     * @return a list of TopBudgetJobDTO with job details and proposal counts
     */
    @Cacheable(value = "S2-F6", key = "#limit", unless = "#result == null or #result.isEmpty()")
    public List<TopBudgetJobDTO> getTopBudgetJobs(int limit) {
        int cappedLimit = Math.min(Math.max(limit, 1), MAX_TOP_BUDGET_LIMIT);
        List<Job> jobs = jobRepository.findByOrderByBudgetMaxDesc(PageRequest.of(0, cappedLimit)).getContent();
        if (jobs.isEmpty()) {
            return List.of();
        }

        Map<Long, ProposalJobSummaryResponse> summariesByJobId =
                fetchAllTimeProposalSummaries(jobs.stream().map(Job::getId).toList());

        return jobs.stream()
                .map(job -> {
                    ProposalJobSummaryResponse summary = summariesByJobId.get(job.getId());
                    Long totalProposals = summary != null && summary.getTotalProposals() != null
                            ? summary.getTotalProposals()
                            : 0L;
                    return new TopBudgetJobDTO(job.getId(), job.getTitle(), job.getBudgetMax(), totalProposals);
                })
                .toList();
    }

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

    private void publishAfterCommit(Runnable publisher) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publisher.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publisher.run();
            }
        });
    }

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
