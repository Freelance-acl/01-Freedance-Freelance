package com.team01.freelance.contract.service;

import com.team01.freelance.contract.model.Contract;
import com.team01.freelance.contract.model.ContractStatus;
import com.team01.freelance.contract.repository.ContractRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContractServiceF6Test {

    private ContractService contractService;
    private ContractRepository contractRepository;

    @BeforeEach
    void setUp() {
        contractService = new ContractService();
        contractRepository = mock(ContractRepository.class);
        ReflectionTestUtils.setField(contractService, "contractRepository", contractRepository);
    }

    @Test
    void historySearchUsesInclusiveDateRangeAndOptionalStatus() {
        List<Contract> activeContracts = List.of(contract(1L, ContractStatus.ACTIVE));
        when(contractRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanAndStatusOrderByCreatedAtAsc(
                LocalDateTime.of(2026, 3, 1, 0, 0),
                LocalDateTime.of(2026, 4, 1, 0, 0),
                ContractStatus.ACTIVE
        )).thenReturn(activeContracts);

        List<Contract> result = contractService.findContractHistory(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                "ACTIVE"
        );

        assertEquals(activeContracts, result);
    }

    @Test
    void historySearchRejectsInvalidDateRange() {
        assertThrows(IllegalArgumentException.class, () -> contractService.findContractHistory(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 3, 31),
                null
        ));
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
