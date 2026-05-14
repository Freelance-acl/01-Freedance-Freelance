package com.team01.freelance.job.controller;

import com.team01.freelance.job.exception.ForbiddenOperationException;
import com.team01.freelance.job.model.JobAttachment;
import com.team01.freelance.job.model.JobAttachmentVerificationRequest;
import com.team01.freelance.job.model.JobRatingRequest;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class JobControllerTest {

    private MockMvc mockMvc;
    private JobService jobService;

    @BeforeEach
    void setUp() {
        JobController controller = new JobController();
        jobService = mock(JobService.class);
        ReflectionTestUtils.setField(controller, "jobService", jobService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
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
    void rateReturnsOk() throws Exception {
        Job job = new Job();
        when(jobService.rateJob(eq(1L), any(JobRatingRequest.class))).thenReturn(job);

        mockMvc.perform(post("/api/jobs/{id}/rate", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contractId\":1,\"rating\":5}"))
                .andExpect(status().isOk());
    }

    @Test
    void rateReturnsBadRequest() throws Exception {
        when(jobService.rateJob(eq(1L), any(JobRatingRequest.class))).thenThrow(new IllegalArgumentException("Invalid rating"));

        mockMvc.perform(post("/api/jobs/{id}/rate", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contractId\":1,\"rating\":6}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rateReturnsNotFound() throws Exception {
        when(jobService.rateJob(eq(1L), any(JobRatingRequest.class))).thenThrow(new EntityNotFoundException("Not found"));

        mockMvc.perform(post("/api/jobs/{id}/rate", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contractId\":1,\"rating\":5}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void verifyAttachmentReturnsOkWithAttachments() throws Exception {
        Job job = new Job();
        JobAttachment attachment = new JobAttachment();
        attachment.setId(2L);
        attachment.setVerified(true);
        attachment.setExpiryDate(LocalDate.now().plusDays(1));
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("verifiedAt", "2026-05-09T10:15:30");
        metadata.put("verifiedBy", 3L);
        attachment.setMetadata(metadata);
        List<JobAttachment> attachments = new ArrayList<>();
        attachments.add(attachment);
        job.setJobAttachments(attachments);

        when(jobService.verifyJobAttachment(eq(1L), eq(2L), any(JobAttachmentVerificationRequest.class))).thenReturn(job);

        mockMvc.perform(put("/api/jobs/{jobId}/attachments/{attachmentId}/verify", 1L, 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verifiedBy\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobAttachments[0].verified").value(true))
                .andExpect(jsonPath("$.jobAttachments[0].metadata.verifiedBy").value(3))
                .andExpect(jsonPath("$.jobAttachments[0].metadata.verifiedAt").exists());
    }

    @Test
    void verifyAttachmentReturnsBadRequest() throws Exception {
        when(jobService.verifyJobAttachment(eq(1L), eq(2L), any(JobAttachmentVerificationRequest.class)))
                .thenThrow(new IllegalArgumentException("Expired"));

        mockMvc.perform(put("/api/jobs/{jobId}/attachments/{attachmentId}/verify", 1L, 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verifiedBy\":3}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifyAttachmentReturnsForbidden() throws Exception {
        when(jobService.verifyJobAttachment(eq(1L), eq(2L), any(JobAttachmentVerificationRequest.class)))
                .thenThrow(new ForbiddenOperationException("Not admin"));

        mockMvc.perform(put("/api/jobs/{jobId}/attachments/{attachmentId}/verify", 1L, 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verifiedBy\":3}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void verifyAttachmentReturnsNotFound() throws Exception {
        when(jobService.verifyJobAttachment(eq(1L), eq(2L), any(JobAttachmentVerificationRequest.class)))
                .thenThrow(new EntityNotFoundException("Not found"));

        mockMvc.perform(put("/api/jobs/{jobId}/attachments/{attachmentId}/verify", 1L, 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verifiedBy\":3}"))
                .andExpect(status().isNotFound());
    }
}
