package com.team01.freelance.contract.service;

import com.team01.freelance.contract.feign.JobServiceClient;
import com.team01.freelance.contract.messaging.ContractEventPublisher;
import com.team01.freelance.contract.model.Contract;
import com.team01.freelance.contract.model.ContractStatus;
import com.team01.freelance.contract.repository.ContractRepository;
import com.team01.freelance.contracts.events.ProposalAcceptedEvent;
import com.team01.freelance.contracts.events.ProposalCancelledEvent;
import com.team01.freelance.contracts.events.ProposalCompletedEvent;
import com.team01.freelance.job.model.Job;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContractServiceSagaEventsTest {

    private ContractService contractService;
    private ContractRepository contractRepository;
    private JobServiceClient jobServiceClient;
    private ContractEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        contractService = new ContractService();
        contractRepository = mock(ContractRepository.class);
        jobServiceClient = mock(JobServiceClient.class);
        eventPublisher = mock(ContractEventPublisher.class);
        ReflectionTestUtils.setField(contractService, "contractRepository", contractRepository);
        ReflectionTestUtils.setField(contractService, "jobServiceClient", jobServiceClient);
        ReflectionTestUtils.setField(contractService, "contractEventPublisher", eventPublisher);
    }

    @Test
    void proposalAcceptedCreatesActiveContractOnce() {
        ProposalAcceptedEvent event = new ProposalAcceptedEvent(10L, 20L, 30L, BigDecimal.valueOf(1250));
        Job job = new Job();
        job.setClientId(40L);
        when(contractRepository.findByProposalId(10L)).thenReturn(Optional.empty());
        when(jobServiceClient.getJob(20L)).thenReturn(job);
        when(contractRepository.saveAndFlush(any(Contract.class))).thenAnswer(invocation -> {
            Contract contract = invocation.getArgument(0);
            contract.setId(99L);
            return contract;
        });

        Contract created = contractService.handleProposalAccepted(event);

        assertEquals(99L, created.getId());
        assertEquals(10L, created.getProposalId());
        assertEquals(20L, created.getJobId());
        assertEquals(30L, created.getFreelancerId());
        assertEquals(40L, created.getClientId());
        assertEquals(ContractStatus.ACTIVE, created.getStatus());
        assertEquals(1250.0, created.getAgreedAmount());
        verify(contractRepository).saveAndFlush(any(Contract.class));
        verify(eventPublisher).publishContractCreated(created);
    }

    @Test
    void proposalAcceptedReturnsExistingContractForDuplicateEvent() {
        Contract existing = new Contract();
        existing.setId(7L);
        when(contractRepository.findByProposalId(10L)).thenReturn(Optional.of(existing));

        Contract result = contractService.handleProposalAccepted(
                new ProposalAcceptedEvent(10L, 20L, 30L, BigDecimal.TEN));

        assertEquals(7L, result.getId());
        verify(jobServiceClient, never()).getJob(any());
        verify(contractRepository, never()).saveAndFlush(any(Contract.class));
    }

    @Test
    void proposalCompletedMarksContractCompleted() {
        Contract contract = contract(5L, 10L, ContractStatus.ACTIVE);
        when(contractRepository.findActiveContractByProposalId(10L)).thenReturn(Optional.of(contract));
        when(contractRepository.saveAndFlush(any(Contract.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Contract updated = contractService.handleProposalCompleted(
                new ProposalCompletedEvent(10L, 20L, 30L, 5L, BigDecimal.valueOf(100)));

        assertEquals(ContractStatus.COMPLETED, updated.getStatus());
        assertTrue(updated.getEndDate() != null);
        verify(eventPublisher).publishContractStatusChanged(5L, ContractStatus.ACTIVE, ContractStatus.COMPLETED);
    }

    @Test
    void proposalCancelledTerminatesContract() {
        Contract contract = contract(5L, 10L, ContractStatus.COMPLETED);
        when(contractRepository.findByProposalId(10L)).thenReturn(Optional.of(contract));
        when(contractRepository.saveAndFlush(any(Contract.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<Contract> result = contractService.handleProposalCancelled(
                new ProposalCancelledEvent(10L, 20L, 30L, "payment_failed"));

        assertTrue(result.isPresent());
        assertEquals(ContractStatus.TERMINATED, result.get().getStatus());
        verify(eventPublisher).publishContractCancelled(result.get());
    }

    private Contract contract(Long contractId, Long proposalId, ContractStatus status) {
        Contract contract = new Contract();
        contract.setId(contractId);
        contract.setProposalId(proposalId);
        contract.setStatus(status);
        return contract;
    }
}
