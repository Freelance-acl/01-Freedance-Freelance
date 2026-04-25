package com.team01.freelance.job.controller;

import com.team01.freelance.job.model.JobAttachment;
import com.team01.freelance.job.service.JobAttachmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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

class JobAttachmentControllerTest {

    private MockMvc mockMvc;
    private JobAttachmentService jobAttachmentService;

    @BeforeEach
    void setUp() {
        JobAttachmentController controller = new JobAttachmentController();
        jobAttachmentService = mock(JobAttachmentService.class);
        ReflectionTestUtils.setField(controller, "jobAttachmentService", jobAttachmentService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getAllReturnsOk() throws Exception {
        when(jobAttachmentService.getAllJobAttachments()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/job-attachments"))
                .andExpect(status().isOk());
    }

    @Test
    void getByIdReturnsOk() throws Exception {
        JobAttachment jobAttachment = new JobAttachment();
        when(jobAttachmentService.getJobAttachmentById(1L)).thenReturn(Optional.of(jobAttachment));

        mockMvc.perform(get("/api/job-attachments/{id}", 1L))
                .andExpect(status().isOk());
    }

    @Test
    void createReturnsOk() throws Exception {
        JobAttachment jobAttachment = new JobAttachment();
        when(jobAttachmentService.createJobAttachment(any(JobAttachment.class))).thenReturn(jobAttachment);

        mockMvc.perform(post("/api/job-attachments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateReturnsOk() throws Exception {
        JobAttachment jobAttachment = new JobAttachment();
        when(jobAttachmentService.updateJobAttachment(eq(1L), any(JobAttachment.class))).thenReturn(Optional.of(jobAttachment));

        mockMvc.perform(put("/api/job-attachments/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteByIdReturnsNoContent() throws Exception {
        when(jobAttachmentService.deleteJobAttachmentById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/job-attachments/{id}", 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteAllReturnsNoContent() throws Exception {
        doNothing().when(jobAttachmentService).deleteAllJobAttachments();

        mockMvc.perform(delete("/api/job-attachments/all"))
                .andExpect(status().isNoContent());
    }
}
