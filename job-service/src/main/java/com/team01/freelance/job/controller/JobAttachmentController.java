package com.team01.freelance.job.controller;

import com.team01.freelance.job.model.JobAttachment;
import com.team01.freelance.job.service.JobAttachmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/job-attachments")
public class JobAttachmentController {

    @Autowired
    private JobAttachmentService jobAttachmentService;

    @GetMapping
    public ResponseEntity<List<JobAttachment>> getAllJobAttachments() {
        return ResponseEntity.ok(jobAttachmentService.getAllJobAttachments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobAttachment> getJobAttachmentById(@PathVariable Long id) {
        return jobAttachmentService.getJobAttachmentById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<JobAttachment> createJobAttachment(@RequestBody JobAttachment jobAttachment) {
        return ResponseEntity.ok(jobAttachmentService.createJobAttachment(jobAttachment));
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobAttachment> updateJobAttachment(@PathVariable Long id, @RequestBody JobAttachment jobAttachment) {
        return jobAttachmentService.updateJobAttachment(id, jobAttachment)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJobAttachmentById(@PathVariable Long id) {
        if (jobAttachmentService.deleteJobAttachmentById(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/all")
    public ResponseEntity<Void> deleteAllJobAttachments() {
        jobAttachmentService.deleteAllJobAttachments();
        return ResponseEntity.noContent().build();
    }
}
