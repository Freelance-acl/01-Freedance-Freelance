package com.team01.freelance.job.service;

import com.team01.freelance.job.model.Job;
import com.team01.freelance.job.repository.JobRepository;
import com.team01.freelance.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class JobService {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

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
}
