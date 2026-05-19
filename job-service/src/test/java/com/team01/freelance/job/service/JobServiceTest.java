package com.team01.freelance.job.service;

import com.team01.freelance.job.dto.JobProposalSummaryDTO;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Test
    void getJobProposalSummaryReturnsProposalsInDateRange() {
        // Arrange
        Long jobId = 1L;
        LocalDate startDate = LocalDate.of(2026, 3, 1);
        LocalDate endDate = LocalDate.of(2026, 3, 31);
        LocalDateTime queryStart = startDate.atStartOfDay();
        LocalDateTime queryEndExclusive = endDate.plusDays(1).atStartOfDay();

        JobProposalSummaryDTO expectedDTO = new JobProposalSummaryDTO(
                jobId,
                "Web Development",
                5L,
                800.0,
                500.0,
                1200.0
        );

        when(jobRepository.existsById(jobId)).thenReturn(true);
        when(jobRepository.getProposalSummary(jobId, queryStart, queryEndExclusive))
                .thenReturn(Optional.of(expectedDTO));

        // Act
        JobProposalSummaryDTO result = jobService.getJobProposalSummary(jobId, startDate, endDate);

        // Assert
        assertNotNull(result);
        assertEquals(5L, result.getTotalProposals());
        assertEquals(800.0, result.getAverageBidAmount());
        assertEquals(500.0, result.getLowestBid());
        assertEquals(1200.0, result.getHighestBid());
        verify(jobRepository).getProposalSummary(jobId, queryStart, queryEndExclusive);
    }

    @Test
    void getJobProposalSummaryReturnsZeroProposalsWhenNoneExist() {
        // Arrange
        Long jobId = 1L;
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31);
        LocalDateTime queryStart = startDate.atStartOfDay();
        LocalDateTime queryEndExclusive = endDate.plusDays(1).atStartOfDay();

        JobProposalSummaryDTO expectedDTO = new JobProposalSummaryDTO(
                jobId,
                "Web Development",
                0L,
                0.0,
                0.0,
                0.0
        );

        when(jobRepository.existsById(jobId)).thenReturn(true);
        when(jobRepository.getProposalSummary(jobId, queryStart, queryEndExclusive))
                .thenReturn(Optional.of(expectedDTO));

        // Act
        JobProposalSummaryDTO result = jobService.getJobProposalSummary(jobId, startDate, endDate);

        // Assert
        assertNotNull(result);
        assertEquals(0L, result.getTotalProposals());
        assertEquals(0.0, result.getAverageBidAmount());
        assertEquals(0.0, result.getLowestBid());
        assertEquals(0.0, result.getHighestBid());
    }

    @Test
    void getJobProposalSummaryThrowsIfJobNotFound() {
        // Arrange
        Long jobId = 999L;
        LocalDate startDate = LocalDate.of(2026, 3, 1);
        LocalDate endDate = LocalDate.of(2026, 3, 31);

        when(jobRepository.existsById(jobId)).thenReturn(false);

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () ->
                jobService.getJobProposalSummary(jobId, startDate, endDate));
        verify(jobRepository, never()).getProposalSummary(any(), any(), any());
    }

    @Test
    void getJobProposalSummaryThrowsIfStartDateAfterEndDate() {
        // Arrange
        Long jobId = 1L;
        LocalDate startDate = LocalDate.of(2026, 3, 31);
        LocalDate endDate = LocalDate.of(2026, 3, 1);

        when(jobRepository.existsById(jobId)).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                jobService.getJobProposalSummary(jobId, startDate, endDate));
        verify(jobRepository, never()).getProposalSummary(any(), any(), any());
    }

    @Test
    void getJobProposalSummaryThrowsIfStartDateIsNull() {
        // Arrange
        Long jobId = 1L;
        LocalDate endDate = LocalDate.of(2026, 3, 31);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                jobService.getJobProposalSummary(jobId, null, endDate));
        verify(jobRepository, never()).existsById(any());
    }

    @Test
    void getJobProposalSummaryThrowsIfEndDateIsNull() {
        // Arrange
        Long jobId = 1L;
        LocalDate startDate = LocalDate.of(2026, 3, 1);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                jobService.getJobProposalSummary(jobId, startDate, null));
        verify(jobRepository, never()).existsById(any());
    }

    @Test
    void getJobProposalSummaryAcceptsSameDateRange() {
        // Arrange
        Long jobId = 1L;
        LocalDate sameDate = LocalDate.of(2026, 3, 15);
        LocalDateTime queryStart = sameDate.atStartOfDay();
        LocalDateTime queryEndExclusive = sameDate.plusDays(1).atStartOfDay();

        JobProposalSummaryDTO expectedDTO = new JobProposalSummaryDTO(
                jobId,
                "Web Development",
                2L,
                750.0,
                700.0,
                800.0
        );

        when(jobRepository.existsById(jobId)).thenReturn(true);
        when(jobRepository.getProposalSummary(jobId, queryStart, queryEndExclusive))
                .thenReturn(Optional.of(expectedDTO));

        // Act
        JobProposalSummaryDTO result = jobService.getJobProposalSummary(jobId, sameDate, sameDate);

        // Assert
        assertNotNull(result);
        assertEquals(2L, result.getTotalProposals());
        verify(jobRepository).getProposalSummary(jobId, queryStart, queryEndExclusive);
    }
}
