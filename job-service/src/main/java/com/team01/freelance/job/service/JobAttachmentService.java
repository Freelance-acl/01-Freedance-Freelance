package com.team01.freelance.job.service;

import com.team01.freelance.job.model.JobAttachment;
import com.team01.freelance.job.repository.JobAttachmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class JobAttachmentService {

    @Autowired
    private JobAttachmentRepository jobAttachmentRepository;

    public List<JobAttachment> getAllJobAttachments() {
        return jobAttachmentRepository.findAll();
    }

    public Optional<JobAttachment> getJobAttachmentById(Long id) {
        return jobAttachmentRepository.findById(id);
    }

    public JobAttachment createJobAttachment(JobAttachment jobAttachment) {
        return jobAttachmentRepository.save(jobAttachment);
    }

    public Optional<JobAttachment> updateJobAttachment(Long id, JobAttachment jobAttachment) {
        return jobAttachmentRepository.findById(id).map(existing -> {
            if (jobAttachment.getType() != null) {
                existing.setType(jobAttachment.getType());
            }
            if (jobAttachment.getFileUrl() != null) {
                existing.setFileUrl(jobAttachment.getFileUrl());
            }
            if (jobAttachment.getExpiryDate() != null) {
                existing.setExpiryDate(jobAttachment.getExpiryDate());
            }
            if (jobAttachment.getVerified() != null) {
                existing.setVerified(jobAttachment.getVerified());
            }
            if (jobAttachment.getMetadata() != null) {
                existing.setMetadata(jobAttachment.getMetadata());
            }
            if (jobAttachment.getJob() != null) {
                existing.setJob(jobAttachment.getJob());
            }
            return jobAttachmentRepository.save(existing);
        });
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
