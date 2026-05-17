package com.team01.freelance.contract.service;

import com.team01.freelance.contract.dto.ContractSummaryDTO;
import com.team01.freelance.contract.model.ContractStatus;
import com.team01.freelance.contract.repository.ContractRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContractServiceF3Test {

    private ContractService contractService;
    private ContractRepository contractRepository;

    @BeforeEach
    void setUp() {
        contractService = new ContractService();
        contractRepository = mock(ContractRepository.class);
        ReflectionTestUtils.setField(contractService, "contractRepository", contractRepository);
    }

    @Test
    void searchContractsMapsJoinedRowsToDto() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 1, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 5, 9, 0);
        when(contractRepository.searchContracts(2000.0, 6000.0, "ACTIVE"))
                .thenReturn(Collections.singletonList(new Object[]{
                        9L, "Nour", "Backend API", 5000.0, "ACTIVE", start, end
                }));

        List<ContractSummaryDTO> result = contractService.searchContracts(2000.0, 6000.0, "ACTIVE");

        assertEquals(1, result.size());
        assertEquals(9L, result.getFirst().getContractId());
        assertEquals("Nour", result.getFirst().getFreelancerName());
        assertEquals("Backend API", result.getFirst().getJobTitle());
        assertEquals(5000.0, result.getFirst().getAgreedAmount());
        assertEquals("ACTIVE", result.getFirst().getStatus());
        assertEquals(4L, result.getFirst().getDurationDays());
    }

    @Test
    void searchContractsRejectsUnknownStatus() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> contractService.searchContracts(100.0, 200.0, "BROKEN"));

        assertInstanceOf(IllegalArgumentException.class, ex);
    }

    @Test
    void searchContractsRejectsInvalidRange() {
        assertThrows(IllegalArgumentException.class,
                () -> contractService.searchContracts(5000.0, 1000.0, ContractStatus.ACTIVE.name()));
    }
}
