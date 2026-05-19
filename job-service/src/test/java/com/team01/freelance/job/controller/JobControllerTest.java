package com.team01.freelance.job.controller;

import com.team01.freelance.job.dto.JobProposalSummaryDTO;
import com.team01.freelance.job.exception.GlobalExceptionHandler;
import com.team01.freelance.job.model.Job;
import com.team01.freelance.job.service.JobService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class JobControllerTest {

    private MockMvc mockMvc;
    private JobService jobService;

    @BeforeEach
    void setUp() {
        JobController controller = new JobController();
        jobService = mock(JobService.class);
        ReflectionTestUtils.setField(controller, "jobService", jobService);
       mockMvc = MockMvcBuilders
        .standaloneSetup(controller)
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
    }

    @Test
    void getAllReturnsOk() throws Exception {
        when(jobService.getAllJobs()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isOk());
    }

    @Test
    void getByIdReturnsOk() throws Exception {
        Job job = new Job();
        when(jobService.getJobById(1L)).thenReturn(Optional.of(job));

        mockMvc.perform(get("/api/jobs/{id}", 1L))
                .andExpect(status().isOk());
    }

    @Test
    void createReturnsOk() throws Exception {
        Job job = new Job();
        when(jobService.createJob(any(Job.class))).thenReturn(job);

        mockMvc.perform(post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateReturnsOk() throws Exception {
        Job job = new Job();
        when(jobService.updateJob(eq(1L), any(Job.class))).thenReturn(job);

        mockMvc.perform(put("/api/jobs/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteByIdReturnsNoContent() throws Exception {
        when(jobService.deleteJobById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/jobs/{id}", 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteAllReturnsNoContent() throws Exception {
        doNothing().when(jobService).deleteAllJobs();

        mockMvc.perform(delete("/api/jobs/all"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getProposalSummaryReturnsOkWithValidDateRange() throws Exception {
        LocalDate startDate = LocalDate.of(2026, 3, 1);
        LocalDate endDate = LocalDate.of(2026, 3, 31);

        JobProposalSummaryDTO dto = new JobProposalSummaryDTO(
                1L,
                "Web Development",
                5L,
                800.0,
                500.0,
                1200.0
        );

        when(jobService.getJobProposalSummary(1L, startDate, endDate)).thenReturn(dto);

        mockMvc.perform(get("/api/jobs/{id}/proposal-summary", 1L)
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31"))
                .andExpect(status().isOk());
    }

    @Test
    void getProposalSummaryReturnsBadRequestForInvalidDateRange() throws Exception {
        LocalDate startDate = LocalDate.of(2026, 3, 31);
        LocalDate endDate = LocalDate.of(2026, 3, 1);

        when(jobService.getJobProposalSummary(1L, startDate, endDate))
                .thenThrow(new IllegalArgumentException("startDate must be on or before endDate"));

        mockMvc.perform(get("/api/jobs/{id}/proposal-summary", 1L)
                        .param("startDate", "2026-03-31")
                        .param("endDate", "2026-03-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getProposalSummaryReturnsNotFoundForNonExistentJob() throws Exception {
        LocalDate startDate = LocalDate.of(2026, 3, 1);
        LocalDate endDate = LocalDate.of(2026, 3, 31);

        when(jobService.getJobProposalSummary(999L, startDate, endDate))
                .thenThrow(new EntityNotFoundException("Job not found with id: 999"));

        mockMvc.perform(get("/api/jobs/{id}/proposal-summary", 999L)
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31"))
                .andExpect(status().isNotFound());
    }
}
