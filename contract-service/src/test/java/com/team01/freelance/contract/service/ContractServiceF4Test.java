package com.team01.freelance.contract.service;

import com.team01.freelance.contract.dto.BatchContractStatusUpdateRequest;
import com.team01.freelance.contract.dto.BatchContractStatusUpdateResponse;
import com.team01.freelance.contract.model.Contract;
import com.team01.freelance.contract.model.ContractStatus;
import com.team01.freelance.contract.repository.ContractRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContractServiceF4Test {

    private ContractService contractService;
    private ContractRepository contractRepository;

    @BeforeEach
    void setUp() {
        contractService = new ContractService();
        contractRepository = mock(ContractRepository.class);
        ReflectionTestUtils.setField(contractService, "contractRepository", contractRepository);
    }

    @Test
    void batchStatusUpdateCompletesContractsAndSetsEndDate() {
        Contract first = contract(1L, ContractStatus.ACTIVE);
        Contract second = contract(2L, ContractStatus.ACTIVE);
        when(contractRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(first, second));
        when(contractRepository.saveAllAndFlush(anyList())).thenReturn(List.of(first, second));

        BatchContractStatusUpdateResponse response = contractService.batchUpdateContractStatus(List.of(
                new BatchContractStatusUpdateRequest(1L, ContractStatus.COMPLETED),
                new BatchContractStatusUpdateRequest(2L, ContractStatus.COMPLETED)
        ));

        assertEquals(2L, response.getCount());
        assertEquals(ContractStatus.COMPLETED, first.getStatus());
        assertEquals(ContractStatus.COMPLETED, second.getStatus());
        assertNotNull(first.getEndDate());
        assertNotNull(second.getEndDate());
        verify(contractRepository).saveAllAndFlush(List.of(first, second));
    }

    @Test
    void batchStatusUpdateThrowsWhenAnyContractMissing() {
        Contract existing = contract(1L, ContractStatus.ACTIVE);
        when(contractRepository.findAllById(List.of(1L, 999L))).thenReturn(List.of(existing));

        assertThrows(EntityNotFoundException.class, () -> contractService.batchUpdateContractStatus(List.of(
                new BatchContractStatusUpdateRequest(1L, ContractStatus.COMPLETED),
                new BatchContractStatusUpdateRequest(999L, ContractStatus.COMPLETED)
        )));
    }

    @Test
    void batchStatusUpdateRejectsInvalidTerminalTransition() {
        Contract completed = contract(1L, ContractStatus.COMPLETED);
        when(contractRepository.findAllById(List.of(1L))).thenReturn(List.of(completed));

        assertThrows(IllegalArgumentException.class, () -> contractService.batchUpdateContractStatus(List.of(
                new BatchContractStatusUpdateRequest(1L, ContractStatus.ACTIVE)
        )));
    }

    private Contract contract(Long id, ContractStatus status) {
        Contract contract = new Contract();
        contract.setId(id);
        contract.setJobId(100L + id);
        contract.setFreelancerId(200L + id);
        contract.setClientId(300L + id);
        contract.setProposalId(400L + id);
        contract.setAgreedAmount(1000.0);
        contract.setStartDate(LocalDateTime.of(2026, 3, 1, 9, 0));
        contract.setCreatedAt(LocalDateTime.of(2026, 3, 1, 9, 0));
        contract.setStatus(status);
        return contract;
    }
}
