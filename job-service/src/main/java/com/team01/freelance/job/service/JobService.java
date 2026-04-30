package com.team01.freelance.job.service;

import com.team01.freelance.job.model.Job;
import com.team01.freelance.job.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class JobService {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    public Optional<Job> getJobById(Long id) {
        return jobRepository.findById(id);
    }

    public Job createJob(Job job) {
        if (job.getBudgetMin() != null && job.getBudgetMax() != null
                && job.getBudgetMin() > job.getBudgetMax()) {
            throw new IllegalArgumentException("Budget minimum cannot be greater than budget maximum");
        }
        validateClientExists(job.getClientId());
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
     * @throws RuntimeException if the job is not found
     */
    public Job updateJob(Long id, Job jobDetails) {
        return jobRepository.findById(id).map(existingJob -> {
                if (jobDetails.getClientId() != null) existingJob.setClientId(jobDetails.getClientId());
                if (jobDetails.getTitle() != null) existingJob.setTitle(jobDetails.getTitle());
                if (jobDetails.getDescription() != null) existingJob.setDescription(jobDetails.getDescription());
                if (jobDetails.getCategory() != null) existingJob.setCategory(jobDetails.getCategory());
                if (jobDetails.getStatus() != null) existingJob.setStatus(jobDetails.getStatus());
                if (jobDetails.getBudgetMin() != null) existingJob.setBudgetMin(jobDetails.getBudgetMin());
                if (jobDetails.getBudgetMax() != null) existingJob.setBudgetMax(jobDetails.getBudgetMax());
                if (jobDetails.getRating() != null) existingJob.setRating(jobDetails.getRating());
                if (jobDetails.getTotalRatings() != null) existingJob.setTotalRatings(jobDetails.getTotalRatings());
                if (jobDetails.getRequirements() != null) existingJob.setRequirements(jobDetails.getRequirements());

            if (existingJob.getBudgetMin() != null && existingJob.getBudgetMax() != null
                    && existingJob.getBudgetMin() > existingJob.getBudgetMax()) {
                throw new IllegalArgumentException("Budget minimum cannot be greater than budget maximum");
            }

            validateClientExists(existingJob.getClientId());


            return jobRepository.save(existingJob);
        }).orElseThrow(() -> new RuntimeException("Job not found with id: " + id));
    }

    private void validateClientExists(Long clientId) {
        if (clientId == null) {
            throw new IllegalArgumentException("Client id is required");
        }

        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM users WHERE id = ?)",
                Boolean.class,
                clientId
        );

        if (!Boolean.TRUE.equals(exists)) {
            throw new IllegalArgumentException("Client not found with id: " + clientId);
        }
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
