package com.team01.freelance.job.controller;

import com.team01.freelance.job.dto.TopBudgetJobDTO;
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

    /**
     * Retrieves the top jobs ordered by budgetMax in descending order.
     * Includes the count of proposals for each job.
     *
     * @param limit the maximum number of jobs to return (default: 10)
     * @return 200 with list of top budget jobs
     */
    /**
     * Retrieves the top jobs ordered by budgetMax in descending order.
     * Includes the count of proposals for each job.
     *
     * @param limit the maximum number of jobs to return (default: 10)
     * @return 200 with list of top budget jobs
     */
    @GetMapping("/reports/top-budget")
    public ResponseEntity<List<TopBudgetJobDTO>> getTopBudgetJobs(
            @RequestParam(name = "limit", defaultValue = "10") int limit) {
        return ResponseEntity.ok(jobService.getTopBudgetJobs(limit));
    }

    @DeleteMapping("/all")
    public ResponseEntity<Void> deleteAllJobs() {
        jobService.deleteAllJobs();
        return ResponseEntity.noContent().build();
    }
}
