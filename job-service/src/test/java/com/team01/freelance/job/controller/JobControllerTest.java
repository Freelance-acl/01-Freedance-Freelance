package com.team01.freelance.job.controller;

import com.team01.freelance.job.dto.JobProposalSummaryDTO;
import com.team01.freelance.job.exception.GlobalExceptionHandler;
import com.team01.freelance.job.dto.TopBudgetJobDTO;
import com.team01.freelance.job.exception.ForbiddenOperationException;
import com.team01.freelance.job.model.JobAttachmentAlertDTO;
import com.team01.freelance.job.model.JobAttachment;
import com.team01.freelance.job.model.JobAttachmentVerificationRequest;
import com.team01.freelance.job.model.JobRatingRequest;
import com.team01.freelance.job.model.JobAttachmentType;
import com.team01.freelance.job.model.Job;
import com.team01.freelance.job.model.JobStatus;
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

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
    void updateRequirementsReturnsOk() throws Exception {
        Job job = new Job();
        when(jobService.updateJobRequirements(eq(1L), any())).thenReturn(job);

        mockMvc.perform(put("/api/jobs/{id}/requirements", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"experienceLevel\":\"SENIOR\",\"duration\":\"8 weeks\"}"))
                .andExpect(status().isOk());

        verify(jobService).updateJobRequirements(eq(1L), any());
    }

    @Test
    void updateRequirementsReturnsNotFoundWhenJobMissing() throws Exception {
        when(jobService.updateJobRequirements(eq(999L), any())).thenThrow(new RuntimeException());

        mockMvc.perform(put("/api/jobs/{id}/requirements", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"experienceLevel\":\"SENIOR\"}"))
                .andExpect(status().isNotFound());
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
    void rateReturnsOk() throws Exception {
        Job job = new Job();
        when(jobService.rateJob(eq(1L), any(JobRatingRequest.class))).thenReturn(job);

        mockMvc.perform(post("/api/jobs/{id}/rate", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contractId\":1,\"rating\":5}"))
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

    @Test
    void getExpiredAttachmentsReturnsOk() throws Exception {
        JobAttachmentAlertDTO jobA = buildAlertDto(1L, "Job A", JobStatus.OPEN, 1);
        JobAttachmentAlertDTO jobC = buildAlertDto(3L, "Job C", JobStatus.CLOSED, 2);
        when(jobService.getJobsWithExpiredAttachments()).thenReturn(List.of(jobA, jobC));

        mockMvc.perform(get("/api/jobs/attachments/expired"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].jobId").value(1))
                .andExpect(jsonPath("$[0].jobTitle").value("Job A"))
                .andExpect(jsonPath("$[0].jobStatus").value("OPEN"))
                .andExpect(jsonPath("$[0].expiredCount").value(1))
                .andExpect(jsonPath("$[0].expiredAttachments.length()").value(1))
                .andExpect(jsonPath("$[1].jobId").value(3))
                .andExpect(jsonPath("$[1].jobTitle").value("Job C"))
                .andExpect(jsonPath("$[1].jobStatus").value("CLOSED"))
                .andExpect(jsonPath("$[1].expiredCount").value(2))
                .andExpect(jsonPath("$[1].expiredAttachments.length()").value(2));
    }

    @Test
    void getExpiredAttachmentsReturnsEmptyList() throws Exception {
        when(jobService.getJobsWithExpiredAttachments()).thenReturn(List.of());

        mockMvc.perform(get("/api/jobs/attachments/expired"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void closeJob_returnsOkWithClosedStatus() throws Exception {
        Job job = new Job();
        job.setId(1L);
        job.setStatus(JobStatus.CLOSED);
        when(jobService.closeJob(1L)).thenReturn(job);

        mockMvc.perform(put("/api/jobs/{id}/close", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void closeJob_returnsBadRequestWhenActiveContract() throws Exception {
        when(jobService.closeJob(1L))
                .thenThrow(new IllegalArgumentException("Cannot close job with an active contract"));

        mockMvc.perform(put("/api/jobs/{id}/close", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\"}"))
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
    void closeJob_returnsBadRequestForInvalidStatus() throws Exception {
        mockMvc.perform(put("/api/jobs/{id}/close", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void closeJob_returnsNotFound() throws Exception {
        when(jobService.closeJob(1L)).thenThrow(new EntityNotFoundException("Job not found with id: 1"));

        mockMvc.perform(put("/api/jobs/{id}/close", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isNotFound());
    }

    // -----------------------------------------------------------------------
    // [S2-F5] Filter Jobs by Requirement (JSONB)
    // -----------------------------------------------------------------------

    @Test
    void searchByRequirements_returnsOkWithMatches() throws Exception {
        Job job = new Job();
        job.setId(1L);
        job.setTitle("Senior Backend");
        when(jobService.searchByRequirements("experienceLevel", "SENIOR", "OPEN"))
                .thenReturn(List.of(job));

        mockMvc.perform(get("/api/jobs/requirements/search")
                        .param("key", "experienceLevel")
                        .param("value", "SENIOR")
                        .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Senior Backend"));
    }

    // -----------------------------------------------------------------------
    // [S2-F6] Top Budget Jobs Report (DTO)
    // -----------------------------------------------------------------------

    @Test
    void getTopBudgetJobs_returnsOkWithOrderedResults() throws Exception {
        TopBudgetJobDTO top = new TopBudgetJobDTO(3L, "Enterprise App", 8000.0, 4L);
        when(jobService.getTopBudgetJobs(2)).thenReturn(List.of(top));

        mockMvc.perform(get("/api/jobs/reports/top-budget").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].jobId").value(3))
                .andExpect(jsonPath("$[0].budgetMax").value(8000.0))
                .andExpect(jsonPath("$[0].totalProposals").value(4));
    }

    private JobAttachmentAlertDTO buildAlertDto(Long jobId, String jobTitle, JobStatus jobStatus, int expiredCount) {
        JobAttachmentAlertDTO dto = new JobAttachmentAlertDTO();
        dto.setJobId(jobId);
        dto.setJobTitle(jobTitle);
        dto.setJobStatus(jobStatus);

        List<JobAttachment> expiredAttachments = new ArrayList<>();
        for (int i = 0; i < expiredCount; i++) {
            JobAttachment attachment = new JobAttachment();
            attachment.setId(jobId * 100 + i);
            attachment.setType(JobAttachmentType.BRIEF);
            attachment.setFileUrl("https://example.com/attachments/" + attachment.getId());
            attachment.setExpiryDate(LocalDate.now().minusDays(i + 1L));
            attachment.setVerified(false);
            expiredAttachments.add(attachment);
        }

        dto.setExpiredAttachments(expiredAttachments);
        dto.setExpiredCount(expiredCount);
        return dto;
    }
}
