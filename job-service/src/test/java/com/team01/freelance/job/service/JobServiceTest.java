package com.team01.freelance.job.service;

import com.team01.freelance.job.model.Job;
import com.team01.freelance.job.repository.JobRepository;
import com.team01.freelance.user.model.User;
import com.team01.freelance.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private JobService jobService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void updateJobThrowsExceptionOnInvalidBudgetRange() {
        // Arrange
        Long jobId = 1L;
        Job existingJob = new Job();
        existingJob.setId(jobId);
        existingJob.setClientId(10L);
        existingJob.setBudgetMin(100.0);
        existingJob.setBudgetMax(200.0);

        Job updateDetails = new Job();
        updateDetails.setBudgetMin(300.0); // 300 > 200

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(existingJob));
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            jobService.updateJob(jobId, updateDetails);
        });
        
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void updateJobSavesOnValidBudgetRange() {
        // Arrange
        Long jobId = 1L;
        Job existingJob = new Job();
        existingJob.setId(jobId);
        existingJob.setClientId(10L);
        existingJob.setBudgetMin(100.0);
        existingJob.setBudgetMax(200.0);

        Job updateDetails = new Job();
        updateDetails.setBudgetMin(150.0);
        updateDetails.setBudgetMax(250.0);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(existingJob));
        when(jobRepository.save(existingJob)).thenReturn(existingJob);
        // Act
        Job result = jobService.updateJob(jobId, updateDetails);

        // Assert
        assertNotNull(result);
        assertEquals(150.0, result.getBudgetMin());
        assertEquals(250.0, result.getBudgetMax());
        verify(jobRepository, times(1)).save(existingJob);
    }

    @Test
    void createJobThrowsIfClientNotFound() {
        Job job = new Job();
        job.setClientId(99L);
        job.setBudgetMin(100.0);
        job.setBudgetMax(200.0);

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> jobService.createJob(job));
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void createJobSavesWhenClientExists() {
        Job job = new Job();
        job.setClientId(10L);
        job.setBudgetMin(100.0);
        job.setBudgetMax(200.0);

        when(userRepository.findById(10L)).thenReturn(Optional.of(new User()));
        when(jobRepository.save(job)).thenReturn(job);

        Job result = jobService.createJob(job);

        assertNotNull(result);
        verify(jobRepository).save(job);
    }
}
