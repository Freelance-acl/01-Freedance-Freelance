package com.team01.freelance.job.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Map;

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

    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    public Optional<Job> getJobById(Long id) {
        return jobRepository.findById(id);
    }

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

        return jobRepository.save(job);
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

            return jobRepository.save(existingJob);
        }).orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + id));
    }

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
            return jobRepository.save(existingJob);
        }).orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + id));
    }

    public boolean deleteJobById(Long id) {
        if (!jobRepository.existsById(id)) {
            return false;
        }
        jobRepository.deleteById(id);
        return true;
    }

    public void deleteAllJobs() {
        jobRepository.deleteAll();
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

    @Transactional
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

        return jobRepository.save(job);
    }

    @Transactional
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
        if (status != null && !status.isEmpty()) {
            return jobRepository.searchByRequirements(key, value, status);
        } else {
            return jobRepository.searchByRequirements(key, value);
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
     * Retrieves the top jobs ordered by budgetMax in descending order.
     * Includes the count of proposals for each job.
     *
     * @param limit the maximum number of jobs to return
     * @return a list of TopBudgetJobDTO with job details and proposal counts
     */
    public List<TopBudgetJobDTO> getTopBudgetJobs(int limit) {
        return jobRepository.findTopBudgetJobs(limit);
    }
}
