package com.team01.freelance.job.service;

import com.team01.freelance.common.observer.EventSubject;
import com.team01.freelance.job.dto.JobProposalSummaryDTO;
import com.team01.freelance.job.client.ContractLookupClient;
import com.team01.freelance.job.client.ContractSummary;
import com.team01.freelance.common.observer.EventSubject;
import com.team01.freelance.job.event.JobEventTypes;
import com.team01.freelance.job.exception.ForbiddenOperationException;
import com.team01.freelance.job.dto.TopBudgetJobDTO;
import com.team01.freelance.job.model.JobAttachmentAlertDTO;
import com.team01.freelance.job.model.JobAttachment;
import com.team01.freelance.job.model.JobAttachmentVerificationRequest;
import com.team01.freelance.job.model.JobAttachmentType;
import com.team01.freelance.job.model.Job;
import com.team01.freelance.job.model.JobRatingRequest;
import com.team01.freelance.job.model.JobStatus;
import com.team01.freelance.job.repository.JobAttachmentRepository;
import com.team01.freelance.job.repository.JobRepository;
import com.team01.freelance.job.search.service.JobSearchIndexOperations;
import com.team01.freelance.user.model.User;
import com.team01.freelance.user.model.UserRole;
import com.team01.freelance.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

import java.util.Arrays;
import java.util.HashMap;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ContractLookupClient contractLookupClient;

    @Mock
    private JobAttachmentRepository jobAttachmentRepository;

    @Mock
    private DataSource dataSource;

    @Mock
    private JobSearchIndexOperations jobSearchIndexOperations;

    private EventSubject jobEventSubject;

    @InjectMocks
    private JobService jobService;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
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
        verify(jobSearchIndexOperations).index(existingJob);
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
        verify(jobSearchIndexOperations).index(job);
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
                .thenReturn(Collections.singletonList(new Object[]{
                        expectedDTO.getJobId(),
                        expectedDTO.getTitle(),
                        expectedDTO.getTotalProposals(),
                        expectedDTO.getAverageBidAmount(),
                        expectedDTO.getLowestBid(),
                        expectedDTO.getHighestBid()
                }));

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
                .thenReturn(Collections.singletonList(new Object[]{
                        expectedDTO.getJobId(),
                        expectedDTO.getTitle(),
                        expectedDTO.getTotalProposals(),
                        expectedDTO.getAverageBidAmount(),
                        expectedDTO.getLowestBid(),
                        expectedDTO.getHighestBid()
                }));

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
                .thenReturn(Collections.singletonList(new Object[]{
                        expectedDTO.getJobId(),
                        expectedDTO.getTitle(),
                        expectedDTO.getTotalProposals(),
                        expectedDTO.getAverageBidAmount(),
                        expectedDTO.getLowestBid(),
                        expectedDTO.getHighestBid()
                }));

        // Act
        JobProposalSummaryDTO result = jobService.getJobProposalSummary(jobId, sameDate, sameDate);

        // Assert
        assertNotNull(result);
        assertEquals(2L, result.getTotalProposals());
        verify(jobRepository).getProposalSummary(jobId, queryStart, queryEndExclusive);
    }

    @Test
    void updateJobRequirementsMergesIncomingFields() {
        Long jobId = 1L;
        Job existingJob = new Job();
        existingJob.setId(jobId);
        Map<String, Object> originalRequirements = new HashMap<>();
        originalRequirements.put("experienceLevel", "MID");
        originalRequirements.put("remote", true);
        originalRequirements.put("skills", Arrays.asList("Java"));
        existingJob.setRequirements(originalRequirements);

        Map<String, Object> updatedFields = new HashMap<>();
        updatedFields.put("experienceLevel", "SENIOR");
        updatedFields.put("duration", "8 weeks");

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(existingJob));
        when(jobRepository.save(existingJob)).thenReturn(existingJob);

        Job result = jobService.updateJobRequirements(jobId, updatedFields);

        assertNotNull(result);
        assertEquals("SENIOR", result.getRequirements().get("experienceLevel"));
        assertEquals(true, result.getRequirements().get("remote"));
        assertEquals(Arrays.asList("Java"), result.getRequirements().get("skills"));
        assertEquals("8 weeks", result.getRequirements().get("duration"));
        verify(jobRepository).save(existingJob);
        verify(jobSearchIndexOperations).index(existingJob);
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(jobEventSubject).notifyObservers(eq(JobEventTypes.REQUIREMENTS_UPDATED_JOB), payloadCaptor.capture());
        Map<String, Object> payload = payloadCaptor.getValue();
        assertEquals(jobId, payload.get("jobId"));
        assertEquals(updatedFields, payload.get("changedRequirements"));
        assertEquals(result.getRequirements(), payload.get("requirements"));
        assertNotNull(payload.get("timestamp"));
    }

    @Test
    void updateJobRequirementsThrowsWhenJobNotFound() {
        when(jobRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> jobService.updateJobRequirements(1L, Map.of("foo", "bar")));
        verify(jobRepository, never()).save(any(Job.class));
        verify(jobEventSubject, never()).notifyObservers(anyString(), any());
    }

    @Test
    void rateJobRecalculatesRunningAverage() {
        Long jobId = 1L;
        Job existingJob = new Job();
        existingJob.setId(jobId);
        existingJob.setRating(0.0);
        existingJob.setTotalRatings(0);

        JobRatingRequest firstRequest = new JobRatingRequest();
        firstRequest.setContractId(1L);
        firstRequest.setRating(5.0);

        JobRatingRequest secondRequest = new JobRatingRequest();
        secondRequest.setContractId(2L);
        secondRequest.setRating(3.0);

        ContractSummary firstContract = new ContractSummary();
        firstContract.setJobId(jobId);
        firstContract.setStatus("COMPLETED");

        ContractSummary secondContract = new ContractSummary();
        secondContract.setJobId(jobId);
        secondContract.setStatus("COMPLETED");

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(existingJob));
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(contractLookupClient.getContractById(1L)).thenReturn(firstContract);
        when(contractLookupClient.getContractById(2L)).thenReturn(secondContract);

        Job firstResult = jobService.rateJob(jobId, firstRequest);

        assertNotNull(firstResult);
        assertEquals(5.0, firstResult.getRating(), 0.0001);
        assertEquals(1, firstResult.getTotalRatings());

        Job secondResult = jobService.rateJob(jobId, secondRequest);

        assertNotNull(secondResult);
        assertEquals(4.0, secondResult.getRating(), 0.0001);
        assertEquals(2, secondResult.getTotalRatings());
        verify(contractLookupClient).getContractById(1L);
        verify(contractLookupClient).getContractById(2L);
        verify(jobRepository, times(2)).save(existingJob);
        verify(jobSearchIndexOperations, times(2)).index(existingJob);
    }

    @Test
    void rateJobRejectsOutOfRangeRating() {
        Long jobId = 1L;
        Job existingJob = new Job();
        existingJob.setId(jobId);

        JobRatingRequest request = new JobRatingRequest();
        request.setContractId(1L);
        request.setRating(6.0);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(existingJob));

        assertThrows(IllegalArgumentException.class, () -> jobService.rateJob(jobId, request));
        verify(contractLookupClient, never()).getContractById(anyLong());
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void rateJobRejectsNonCompletedContract() {
        Long jobId = 1L;
        Job existingJob = new Job();
        existingJob.setId(jobId);

        JobRatingRequest request = new JobRatingRequest();
        request.setContractId(1L);
        request.setRating(5.0);

        ContractSummary contract = new ContractSummary();
        contract.setJobId(jobId);
        contract.setStatus("ACTIVE");

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(existingJob));
        when(contractLookupClient.getContractById(1L)).thenReturn(contract);

        assertThrows(IllegalArgumentException.class, () -> jobService.rateJob(jobId, request));
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void rateJobRejectsContractForAnotherJob() {
        Long jobId = 1L;
        Job existingJob = new Job();
        existingJob.setId(jobId);

        JobRatingRequest request = new JobRatingRequest();
        request.setContractId(1L);
        request.setRating(5.0);

        ContractSummary contract = new ContractSummary();
        contract.setJobId(2L);
        contract.setStatus("COMPLETED");

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(existingJob));
        when(contractLookupClient.getContractById(1L)).thenReturn(contract);

        assertThrows(IllegalArgumentException.class, () -> jobService.rateJob(jobId, request));
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void rateJobThrowsWhenContractIsMissing() {
        Long jobId = 1L;
        Job existingJob = new Job();
        existingJob.setId(jobId);

        JobRatingRequest request = new JobRatingRequest();
        request.setContractId(1L);
        request.setRating(5.0);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(existingJob));
        when(contractLookupClient.getContractById(1L)).thenThrow(new EntityNotFoundException("Contract not found with id: 1"));

        assertThrows(EntityNotFoundException.class, () -> jobService.rateJob(jobId, request));
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void verifyJobAttachmentUpdatesMetadataAndReturnsJobWithAttachments() {
        Long jobId = 1L;
        Long attachmentId = 10L;
        Long verifierId = 3L;

        Job job = new Job();
        job.setId(jobId);

        JobAttachment attachment = new JobAttachment();
        attachment.setId(attachmentId);
        attachment.setExpiryDate(LocalDate.now().plusDays(1));
        attachment.setVerified(false);
        attachment.setMetadata(new LinkedHashMap<>(Map.of("existing", "value")));
        attachment.setJob(job);

        List<JobAttachment> attachments = new ArrayList<>();
        attachments.add(attachment);
        job.setJobAttachments(attachments);

        User verifier = new User();
        verifier.setRole(UserRole.ADMIN);

        JobAttachmentVerificationRequest request = new JobAttachmentVerificationRequest();
        request.setVerifiedBy(verifierId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobAttachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));
        when(userRepository.findById(verifierId)).thenReturn(Optional.of(verifier));
        when(jobAttachmentRepository.save(any(JobAttachment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Job result = jobService.verifyJobAttachment(jobId, attachmentId, request);

        assertNotNull(result);
        assertEquals(1, result.getJobAttachments().size());
        assertTrue(result.getJobAttachments().get(0).getVerified());
        assertEquals(verifierId, result.getJobAttachments().get(0).getMetadata().get("verifiedBy"));
        assertTrue(result.getJobAttachments().get(0).getMetadata().containsKey("verifiedAt"));
        verify(jobAttachmentRepository).save(attachment);
    }

    @Test
    void verifyJobAttachmentRejectsExpiredAttachment() {
        Long jobId = 1L;
        Long attachmentId = 10L;

        Job job = new Job();
        job.setId(jobId);

        JobAttachment attachment = new JobAttachment();
        attachment.setId(attachmentId);
        attachment.setExpiryDate(LocalDate.now().minusDays(1));
        attachment.setJob(job);

        JobAttachmentVerificationRequest request = new JobAttachmentVerificationRequest();
        request.setVerifiedBy(3L);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobAttachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));

        assertThrows(IllegalArgumentException.class, () -> jobService.verifyJobAttachment(jobId, attachmentId, request));
        verify(jobAttachmentRepository, never()).save(any(JobAttachment.class));
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    void verifyJobAttachmentRejectsDifferentJob() {
        Long jobId = 1L;
        Long attachmentId = 10L;

        Job job = new Job();
        job.setId(jobId);

        Job otherJob = new Job();
        otherJob.setId(2L);

        JobAttachment attachment = new JobAttachment();
        attachment.setId(attachmentId);
        attachment.setExpiryDate(LocalDate.now().plusDays(1));
        attachment.setJob(otherJob);

        JobAttachmentVerificationRequest request = new JobAttachmentVerificationRequest();
        request.setVerifiedBy(3L);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobAttachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));

        assertThrows(IllegalArgumentException.class, () -> jobService.verifyJobAttachment(jobId, attachmentId, request));
        verify(jobAttachmentRepository, never()).save(any(JobAttachment.class));
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    void verifyJobAttachmentRejectsNonAdminVerifier() {
        Long jobId = 1L;
        Long attachmentId = 10L;

        Job job = new Job();
        job.setId(jobId);

        JobAttachment attachment = new JobAttachment();
        attachment.setId(attachmentId);
        attachment.setExpiryDate(LocalDate.now().plusDays(1));
        attachment.setJob(job);

        User verifier = new User();
        verifier.setRole(UserRole.CLIENT);

        JobAttachmentVerificationRequest request = new JobAttachmentVerificationRequest();
        request.setVerifiedBy(3L);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobAttachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));
        when(userRepository.findById(3L)).thenReturn(Optional.of(verifier));

        assertThrows(ForbiddenOperationException.class, () -> jobService.verifyJobAttachment(jobId, attachmentId, request));
        verify(jobAttachmentRepository, never()).save(any(JobAttachment.class));
    }

    @Test
    void verifyJobAttachmentThrowsWhenJobMissing() {
        JobAttachmentVerificationRequest request = new JobAttachmentVerificationRequest();
        request.setVerifiedBy(3L);

        when(jobRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> jobService.verifyJobAttachment(1L, 10L, request));
        verify(jobAttachmentRepository, never()).findById(anyLong());
    }

    @Test
    void verifyJobAttachmentThrowsWhenAttachmentMissing() {
        Job job = new Job();
        job.setId(1L);

        JobAttachmentVerificationRequest request = new JobAttachmentVerificationRequest();
        request.setVerifiedBy(3L);

        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(jobAttachmentRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> jobService.verifyJobAttachment(1L, 10L, request));
        verify(userRepository, never()).findById(anyLong());
        verify(jobAttachmentRepository, never()).save(any(JobAttachment.class));
    }

    @Test
    void getJobsWithExpiredAttachmentsReturnsExpiredReportsOnly() {
        Job jobA = createJobWithAttachments(1L, "Job A", JobStatus.OPEN,
                createAttachment(101L, LocalDate.now().minusDays(1)),
                createAttachment(102L, LocalDate.now().plusDays(1)));

        Job jobB = createJobWithAttachments(2L, "Job B", JobStatus.IN_PROGRESS,
                createAttachment(201L, LocalDate.now().plusDays(2)),
                createAttachment(202L, LocalDate.now().plusDays(3)));

        Job jobC = createJobWithAttachments(3L, "Job C", JobStatus.CLOSED,
                createAttachment(301L, LocalDate.now().minusDays(1)),
                createAttachment(302L, LocalDate.now().minusDays(2)));

        when(jobRepository.findJobsWithExpiredAttachments()).thenReturn(List.of(jobA, jobB, jobC));

        List<JobAttachmentAlertDTO> result = jobService.getJobsWithExpiredAttachments();

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getJobId());
        assertEquals("Job A", result.get(0).getJobTitle());
        assertEquals(JobStatus.OPEN, result.get(0).getJobStatus());
        assertEquals(1, result.get(0).getExpiredCount());
        assertEquals(1, result.get(0).getExpiredAttachments().size());

        assertEquals(3L, result.get(1).getJobId());
        assertEquals("Job C", result.get(1).getJobTitle());
        assertEquals(JobStatus.CLOSED, result.get(1).getJobStatus());
        assertEquals(2, result.get(1).getExpiredCount());
        assertEquals(2, result.get(1).getExpiredAttachments().size());

        assertTrue(result.stream().noneMatch(dto -> dto.getJobId().equals(2L)));
        verify(jobRepository).findJobsWithExpiredAttachments();
    }

    @Test
    void getJobsWithExpiredAttachmentsReturnsEmptyListWhenNoneExist() {
        when(jobRepository.findJobsWithExpiredAttachments()).thenReturn(List.of());

        List<JobAttachmentAlertDTO> result = jobService.getJobsWithExpiredAttachments();

        assertTrue(result.isEmpty());
        verify(jobRepository).findJobsWithExpiredAttachments();
    }

    @Test
    void closeJob_usesAtomicUpdateAndRejectsProposals() {
        Long jobId = 1L;
        Job openJob = new Job();
        openJob.setId(jobId);
        openJob.setStatus(JobStatus.OPEN);

        Job closedJob = new Job();
        closedJob.setId(jobId);
        closedJob.setStatus(JobStatus.CLOSED);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(openJob), Optional.of(closedJob));
        when(jobRepository.closeJobIfEligible(jobId)).thenReturn(1);

        Job result = jobService.closeJob(jobId);

        assertEquals(JobStatus.CLOSED, result.getStatus());
        verify(jobRepository).closeJobIfEligible(jobId);
        verify(jobRepository).rejectSubmittedProposalsByJobId(jobId);
        verify(jobEventSubject).notifyObservers(eq("JOB_CLOSED"), any(Map.class));
        verify(jobRepository, never()).save(any(Job.class));
        verify(jobSearchIndexOperations).index(closedJob);
    }

    @Test
    void closeJob_throwsWhenAtomicUpdateBlocked() {
        Long jobId = 1L;
        Job openJob = new Job();
        openJob.setId(jobId);
        openJob.setStatus(JobStatus.OPEN);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(openJob));
        when(jobRepository.closeJobIfEligible(jobId)).thenReturn(0);

        assertThrows(IllegalArgumentException.class, () -> jobService.closeJob(jobId));
        verify(jobRepository, never()).rejectSubmittedProposalsByJobId(anyLong());
        verify(jobEventSubject, never()).notifyObservers(anyString(), any());
    }

    @Test
    void searchJobsByStatusAndBudgetRange_delegatesToRepository() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        org.springframework.data.domain.Page<Job> page = new org.springframework.data.domain.PageImpl<>(List.of(new Job()));
        when(jobRepository.searchJobsByStatusAndBudgetRange("OPEN", 100.0, 500.0, pageable)).thenReturn(page);

        org.springframework.data.domain.Page<Job> result =
                jobService.searchJobsByStatusAndBudgetRange("OPEN", 100.0, 500.0, pageable);

        assertEquals(1, result.getTotalElements());
        verify(jobRepository).searchJobsByStatusAndBudgetRange("OPEN", 100.0, 500.0, pageable);
    }

    @Test
    void searchJobsByStatusAndBudgetRange_rejectsInvertedRange() {
        assertThrows(IllegalArgumentException.class,
                () -> jobService.searchJobsByStatusAndBudgetRange(null, 5000.0, 1000.0,
                        org.springframework.data.domain.PageRequest.of(0, 10)));
    }

    @Test
    void searchByRequirements_withStatus_delegatesToRepositoryWithStatus() {
        Job job = new Job();
        when(jobRepository.searchByRequirements("experienceLevel", "SENIOR", "OPEN"))
                .thenReturn(List.of(job));

        List<Job> result = jobService.searchByRequirements("experienceLevel", "SENIOR", "OPEN");

        assertEquals(1, result.size());
        verify(jobRepository).searchByRequirements("experienceLevel", "SENIOR", "OPEN");
        verify(jobRepository, never()).searchByRequirements("experienceLevel", "SENIOR");
    }

    @Test
    void searchByRequirements_withoutStatus_delegatesToRepositoryWithoutStatus() {
        when(jobRepository.searchByRequirements("experienceLevel", "SENIOR"))
                .thenReturn(List.of());

        jobService.searchByRequirements("experienceLevel", "SENIOR", null);

        verify(jobRepository).searchByRequirements("experienceLevel", "SENIOR");
    }

    @Test
    void getTopBudgetJobs_returnsRepositoryResults() {
        TopBudgetJobDTO dto = new TopBudgetJobDTO(1L, "Job A", 5000.0, 2L);
        when(jobRepository.findTopBudgetJobs(2)).thenReturn(List.of(dto));

        List<TopBudgetJobDTO> result = jobService.getTopBudgetJobs(2);

        assertEquals(1, result.size());
        assertNotSame(dto, result.getFirst());
        assertEquals(1L, result.getFirst().getJobId());
        assertEquals("Job A", result.getFirst().getTitle());
        assertEquals(5000.0, result.getFirst().getBudgetMax());
        assertEquals(2L, result.getFirst().getTotalProposals());
        verify(jobRepository).findTopBudgetJobs(2);
    }

    @Test
    void closeJob_returnsEarlyWhenAlreadyClosed() {
        Long jobId = 1L;
        Job closedJob = new Job();
        closedJob.setId(jobId);
        closedJob.setStatus(JobStatus.CLOSED);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(closedJob));

        Job result = jobService.closeJob(jobId);

        assertEquals(JobStatus.CLOSED, result.getStatus());
        verify(jobRepository, never()).closeJobIfEligible(anyLong());
        verify(jobSearchIndexOperations, never()).index(any(Job.class));
        verify(jobEventSubject, never()).notifyObservers(anyString(), any());
    }

    private Job createJobWithAttachments(Long id, String title, JobStatus status, JobAttachment... attachments) {
        Job job = new Job();
        job.setId(id);
        job.setTitle(title);
        job.setStatus(status);

        List<JobAttachment> jobAttachments = new ArrayList<>();
        for (JobAttachment attachment : attachments) {
            jobAttachments.add(attachment);
        }
        job.setJobAttachments(jobAttachments);
        return job;
    }

    private JobAttachment createAttachment(Long id, LocalDate expiryDate) {
        JobAttachment attachment = new JobAttachment();
        attachment.setId(id);
        attachment.setType(JobAttachmentType.BRIEF);
        attachment.setFileUrl("https://example.com/attachments/" + id);
        attachment.setExpiryDate(expiryDate);
        attachment.setVerified(false);
        return attachment;
    }
}
