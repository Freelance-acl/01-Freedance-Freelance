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
        if (!jobAttachmentRepository.existsById(id)) {
            return Optional.empty();
        }
        jobAttachment.setId(id);
        return Optional.of(jobAttachmentRepository.save(jobAttachment));
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
