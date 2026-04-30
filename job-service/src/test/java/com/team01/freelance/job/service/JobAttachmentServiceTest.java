package com.team01.freelance.job.service;

import com.team01.freelance.job.model.JobAttachment;
import com.team01.freelance.job.model.JobAttachmentType;
import com.team01.freelance.job.repository.JobAttachmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JobAttachmentServiceTest {

    @Mock
    private JobAttachmentRepository jobAttachmentRepository;

    @InjectMocks
    private JobAttachmentService jobAttachmentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void updateJobAttachment_ShouldMergeNonNullFields() {
        // Arrange
        Long id = 1L;
        JobAttachment existing = new JobAttachment();
        existing.setId(id);
        existing.setType(JobAttachmentType.BRIEF);
        existing.setFileUrl("http://old.url");
        existing.setExpiryDate(LocalDate.now().plusDays(10));
        existing.setVerified(true);

        JobAttachment incoming = new JobAttachment();
        incoming.setFileUrl("http://new.url");
        // type, expiryDate, verified are null in incoming

        when(jobAttachmentRepository.existsById(id)).thenReturn(true);
        when(jobAttachmentRepository.findById(id)).thenReturn(Optional.of(existing));
        when(jobAttachmentRepository.save(any(JobAttachment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Optional<JobAttachment> result = jobAttachmentService.updateJobAttachment(id, incoming);

        // Assert
        assertTrue(result.isPresent());
        JobAttachment updated = result.get();
        assertEquals(id, updated.getId());
        assertEquals("http://new.url", updated.getFileUrl()); // Updated
        assertEquals(JobAttachmentType.BRIEF, updated.getType()); // Preserved
        assertEquals(existing.getExpiryDate(), updated.getExpiryDate()); // Preserved
        assertTrue(updated.getVerified()); // Preserved
        
        verify(jobAttachmentRepository).findById(id);
        verify(jobAttachmentRepository).save(updated);
    }

    @Test
    void updateJobAttachment_NowMergesInsteadOfOverwriting() {
        // This test verifies the NEW behavior
        
        Long id = 1L;
        JobAttachment existing = new JobAttachment();
        existing.setId(id);
        existing.setType(JobAttachmentType.BRIEF);
        existing.setFileUrl("http://old.url");

        JobAttachment incoming = new JobAttachment();
        incoming.setFileUrl("http://new.url");
        // other fields are null

        when(jobAttachmentRepository.findById(id)).thenReturn(Optional.of(existing));
        when(jobAttachmentRepository.save(any(JobAttachment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Optional<JobAttachment> result = jobAttachmentService.updateJobAttachment(id, incoming);

        // Assert
        assertTrue(result.isPresent());
        JobAttachment updated = result.get();
        assertEquals("http://new.url", updated.getFileUrl());
        assertEquals(JobAttachmentType.BRIEF, updated.getType()); // No longer null
    }
}
