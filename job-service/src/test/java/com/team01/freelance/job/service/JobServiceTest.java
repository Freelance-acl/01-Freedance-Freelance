package com.team01.freelance.job.service;

import com.team01.freelance.job.client.ContractLookupClient;
import com.team01.freelance.job.client.ContractSummary;
import com.team01.freelance.job.exception.ForbiddenOperationException;
import com.team01.freelance.job.model.JobAttachment;
import com.team01.freelance.job.model.JobAttachmentVerificationRequest;
import com.team01.freelance.job.model.Job;
import com.team01.freelance.job.model.JobRatingRequest;
import com.team01.freelance.job.repository.JobAttachmentRepository;
import com.team01.freelance.job.repository.JobRepository;
import com.team01.freelance.user.model.User;
import com.team01.freelance.user.model.UserRole;
import com.team01.freelance.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.ArrayList;
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
}
