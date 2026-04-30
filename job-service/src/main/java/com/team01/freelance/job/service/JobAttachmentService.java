package com.team01.freelance.job.service;

import com.team01.freelance.job.model.JobAttachment;
import com.team01.freelance.job.repository.JobAttachmentRepository;
import com.team01.freelance.job.repository.JobRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class JobAttachmentService {

    @Autowired
    private JobAttachmentRepository jobAttachmentRepository;

    @Autowired
    private JobRepository jobRepository;

    public List<JobAttachment> getAllJobAttachments() {
        return jobAttachmentRepository.findAll();
    }

    public Optional<JobAttachment> getJobAttachmentById(Long id) {
        return jobAttachmentRepository.findById(id);
    }

    public JobAttachment createJobAttachment(JobAttachment jobAttachment) {
        if (jobAttachment.getJob() != null && jobAttachment.getJob().getId() != null) {
            jobAttachment.setJob(jobRepository.findById(jobAttachment.getJob().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + jobAttachment.getJob().getId())));
        }
        return jobAttachmentRepository.save(jobAttachment);
    }

    /**
     * Updates an existing job attachment and throws if it does not exist.
     * Validates the existence of the associated job if provided.
     *
     * @param id The ID of the job attachment to update
     * @param jobAttachment The object containing updated fields
     * @return The updated job attachment
     * @throws EntityNotFoundException if the job attachment or associated job is not found
     */
    public JobAttachment updateJobAttachment(Long id, JobAttachment jobAttachment) {
        return jobAttachmentRepository.findById(id).map(existing -> {
                if (jobAttachment.getType() != null) existing.setType(jobAttachment.getType());
                if (jobAttachment.getFileUrl() != null) existing.setFileUrl(jobAttachment.getFileUrl());
                if (jobAttachment.getExpiryDate() != null) existing.setExpiryDate(jobAttachment.getExpiryDate());
                if (jobAttachment.getVerified() != null) existing.setVerified(jobAttachment.getVerified());
                if (jobAttachment.getMetadata() != null) existing.setMetadata(jobAttachment.getMetadata());
                if (jobAttachment.getJob() != null && jobAttachment.getJob().getId() != null) {
                    existing.setJob(jobRepository.findById(jobAttachment.getJob().getId())
                            .orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + jobAttachment.getJob().getId())));
                }
            return jobAttachmentRepository.save(existing);
        }).orElseThrow(() -> new EntityNotFoundException("Job Attachment not found with id: " + id));
    }

    public boolean deleteJobAttachmentById(Long id) {
        if (!jobAttachmentRepository.existsById(id)) {
            return false;
        }
        jobAttachmentRepository.deleteById(id);
        return true;
    }

    public void deleteAllJobAttachments() {
        jobAttachmentRepository.deleteAll();
    }
}
