package com.team01.freelance.job.service;

import com.team01.freelance.job.model.Job;
import com.team01.freelance.job.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

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
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any())).thenReturn(true);

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
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any())).thenReturn(true);

        // Act
        Job result = jobService.updateJob(jobId, updateDetails);

        // Assert
        assertNotNull(result);
        assertEquals(150.0, result.getBudgetMin());
        assertEquals(250.0, result.getBudgetMax());
        verify(jobRepository, times(1)).save(existingJob);
    }
}
