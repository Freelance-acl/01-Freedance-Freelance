package com.team01.freelance.job.controller;

import com.team01.freelance.job.dto.JobProposalSummaryDTO;
import com.team01.freelance.job.model.Job;
import com.team01.freelance.job.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    @Autowired
    private JobService jobService;

    @GetMapping
    public ResponseEntity<List<Job>> getAllJobs() {
        return ResponseEntity.ok(jobService.getAllJobs());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Job> getJobById(@PathVariable Long id) {
        return jobService.getJobById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * [S2-F3] Retrieves job proposal summary statistics within a date range.
     *
     * @param id the job ID
     * @param startDate inclusive start date (ISO 8601 format: YYYY-MM-DD)
     * @param endDate inclusive end date (ISO 8601 format: YYYY-MM-DD)
     * @return 200 with JobProposalSummaryDTO, 400 for invalid date range, or 404 if job not found
     */
    @GetMapping("/{id}/proposal-summary")
    public ResponseEntity<JobProposalSummaryDTO> getJobProposalSummary(
            @PathVariable Long id,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        return ResponseEntity.ok(jobService.getJobProposalSummary(id, startDate, endDate));
    }

    @PostMapping
    public ResponseEntity<Job> createJob(@RequestBody Job job) {
        return ResponseEntity.ok(jobService.createJob(job));
    }

    /**
     * Updates a job by ID.
     *
     * @param id the job ID
     * @param job the update payload
     * @return 200 with updated job, or 404 if not found
     */
    @PutMapping("/{id}")
    public ResponseEntity<Job> updateJob(@PathVariable Long id, @RequestBody Job job) {
        try {
            return ResponseEntity.ok(jobService.updateJob(id, job));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJobById(@PathVariable Long id) {
        if (jobService.deleteJobById(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/all")
    public ResponseEntity<Void> deleteAllJobs() {
        jobService.deleteAllJobs();
        return ResponseEntity.noContent().build();
    }
}
