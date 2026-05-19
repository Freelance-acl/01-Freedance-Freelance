package com.team01.freelance.contract.service;

import com.team01.freelance.contract.model.Contract;
import com.team01.freelance.contract.model.ContractStatus;
import com.team01.freelance.contract.repository.ContractRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContractServiceF5Test {

    private ContractService contractService;
    private ContractRepository contractRepository;

    @BeforeEach
    void setUp() {
        contractService = new ContractService();
        contractRepository = mock(ContractRepository.class);
        ReflectionTestUtils.setField(contractService, "contractRepository", contractRepository);
    }

    @Test
    void metadataSearchDelegatesByOperator() {
        List<Contract> matches = List.of(contract(1L));
        when(contractRepository.findByMetadataGreaterThan("progressPercentage", 40.0)).thenReturn(matches);

        List<Contract> result = contractService.findContractsByMetadata("progressPercentage", "gt", "40");

        assertEquals(matches, result);
        verify(contractRepository).findByMetadataGreaterThan("progressPercentage", 40.0);
    }

    @Test
    void metadataSearchRejectsUnknownOperator() {
        assertThrows(IllegalArgumentException.class,
                () -> contractService.findContractsByMetadata("progressPercentage", "xyz", "50"));
    }

    private Contract contract(Long id) {
        Contract contract = new Contract();
        contract.setId(id);
        contract.setJobId(100L + id);
        contract.setFreelancerId(200L + id);
        contract.setClientId(300L + id);
        contract.setProposalId(400L + id);
        contract.setAgreedAmount(1000.0);
        contract.setStartDate(LocalDateTime.of(2026, 3, 1, 9, 0));
        contract.setCreatedAt(LocalDateTime.of(2026, 3, 1, 9, 0));
        contract.setStatus(ContractStatus.ACTIVE);
        return contract;
    }
}
