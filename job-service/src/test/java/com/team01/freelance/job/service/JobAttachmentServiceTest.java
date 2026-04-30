package com.team01.freelance.job.service;

import com.team01.freelance.job.model.Job;
import com.team01.freelance.job.model.JobAttachment;
import com.team01.freelance.job.model.JobAttachmentType;
import com.team01.freelance.job.repository.JobAttachmentRepository;
import com.team01.freelance.job.repository.JobRepository;
import jakarta.persistence.EntityNotFoundException;
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

    @Mock
    private JobRepository jobRepository;

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

        when(jobAttachmentRepository.findById(id)).thenReturn(Optional.of(existing));
        when(jobAttachmentRepository.save(any(JobAttachment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        JobAttachment result = jobAttachmentService.updateJobAttachment(id, incoming);

        // Assert
        assertNotNull(result);
        JobAttachment updated = result;
        assertEquals(id, updated.getId());
        assertEquals("http://new.url", updated.getFileUrl()); // Updated
        assertEquals(JobAttachmentType.BRIEF, updated.getType()); // Preserved
        assertEquals(existing.getExpiryDate(), updated.getExpiryDate()); // Preserved
        assertTrue(updated.getVerified()); // Preserved
        
        verify(jobAttachmentRepository).findById(id);
        verify(jobAttachmentRepository).save(updated);
    }

    @Test
    void updateJobAttachment_ShouldValidateJobIfProvided() {
        // Arrange
        Long id = 1L;
        JobAttachment existing = new JobAttachment();
        existing.setId(id);
        
        JobAttachment incoming = new JobAttachment();
        Job job = new Job();
        job.setId(100L);
        incoming.setJob(job);

        when(jobAttachmentRepository.findById(id)).thenReturn(Optional.of(existing));
        when(jobRepository.findById(100L)).thenReturn(Optional.of(job));
        when(jobAttachmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Act
        JobAttachment result = jobAttachmentService.updateJobAttachment(id, incoming);

        // Assert
        assertNotNull(result);
        verify(jobRepository).findById(100L);
    }

    @Test
    void updateJobAttachment_ShouldThrowIfJobNotFound() {
        // Arrange
        Long id = 1L;
        JobAttachment existing = new JobAttachment();
        existing.setId(id);
        
        JobAttachment incoming = new JobAttachment();
        Job job = new Job();
        job.setId(100L);
        incoming.setJob(job);

        when(jobAttachmentRepository.findById(id)).thenReturn(Optional.of(existing));
        when(jobRepository.findById(100L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> jobAttachmentService.updateJobAttachment(id, incoming));
    }

    @Test
    void createJobAttachment_ShouldValidateJob() {
        // Arrange
        JobAttachment attachment = new JobAttachment();
        Job job = new Job();
        job.setId(100L);
        attachment.setJob(job);

        when(jobRepository.findById(100L)).thenReturn(Optional.of(job));
        when(jobAttachmentRepository.save(attachment)).thenReturn(attachment);

        // Act
        JobAttachment result = jobAttachmentService.createJobAttachment(attachment);

        // Assert
        assertNotNull(result);
        verify(jobRepository).findById(100L);
    }

    @Test
    void createJobAttachment_ShouldThrowIfJobNotFound() {
        // Arrange
        JobAttachment attachment = new JobAttachment();
        Job job = new Job();
        job.setId(100L);
        attachment.setJob(job);

        when(jobRepository.findById(100L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> jobAttachmentService.createJobAttachment(attachment));
    }
}
