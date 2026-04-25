package com.team01.freelance.job.service;

import com.team01.freelance.job.model.Job;
import com.team01.freelance.job.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class JobService {

    @Autowired
    private JobRepository jobRepository;

    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    public Optional<Job> getJobById(Long id) {
        return jobRepository.findById(id);
    }

    public Job createJob(Job job) {
        return jobRepository.save(job);
    }

    public Optional<Job> updateJob(Long id, Job jobDetails) {
        return jobRepository.findById(id).map(existingJob -> {
            if (jobDetails.getClientId() != null) {
                existingJob.setClientId(jobDetails.getClientId());
            }
            if (jobDetails.getTitle() != null) {
                existingJob.setTitle(jobDetails.getTitle());
            }
            if (jobDetails.getDescription() != null) {
                existingJob.setDescription(jobDetails.getDescription());
            }
            if (jobDetails.getCategory() != null) {
                existingJob.setCategory(jobDetails.getCategory());
            }
            if (jobDetails.getStatus() != null) {
                existingJob.setStatus(jobDetails.getStatus());
            }
            if (jobDetails.getBudgetMin() != null) {
                existingJob.setBudgetMin(jobDetails.getBudgetMin());
            }
            if (jobDetails.getBudgetMax() != null) {
                existingJob.setBudgetMax(jobDetails.getBudgetMax());
            }
            if (jobDetails.getRating() != null) {
                existingJob.setRating(jobDetails.getRating());
            }
            if (jobDetails.getTotalRatings() != null) {
                existingJob.setTotalRatings(jobDetails.getTotalRatings());
            }
            if (jobDetails.getRequirements() != null) {
                existingJob.setRequirements(jobDetails.getRequirements());
            }
            return jobRepository.save(existingJob);
        });
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
