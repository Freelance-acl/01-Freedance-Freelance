package com.team01.freelance.contract.service;

import com.team01.freelance.contract.dto.ContractSummaryDTO;
import com.team01.freelance.contract.model.Contract;
import com.team01.freelance.contract.model.ContractStatus;
import com.team01.freelance.contract.repository.ContractRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContractServiceF123Test {

    private ContractService contractService;
    private ContractRepository contractRepository;

    @BeforeEach
    void setUp() {
        contractService = new ContractService();
        contractRepository = mock(ContractRepository.class);
        ReflectionTestUtils.setField(contractService, "contractRepository", contractRepository);
    }

    @Test
    void getActiveContractForUserReturnsMostRecentActiveContract() {
        Contract contract = new Contract();
        contract.setId(77L);

        when(contractRepository.userExists(10L)).thenReturn(true);
        when(contractRepository.findMostRecentActiveContractForUser(10L)).thenReturn(Optional.of(contract));

        Contract result = contractService.getActiveContractForUser(10L);

        assertEquals(77L, result.getId());
    }

    @Test
    void getActiveContractForUserThrowsWhenUserMissing() {
        when(contractRepository.userExists(404L)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> contractService.getActiveContractForUser(404L));
    }

    @Test
    void updateContractProgressMergesMetadataWithoutDroppingExistingFields() {
        Contract contract = new Contract();
        contract.setId(5L);
        contract.setMetadata(new LinkedHashMap<>(Map.of(
                "existingKey", "keep",
                "progressPercentage", 10
        )));

        when(contractRepository.findById(5L)).thenReturn(Optional.of(contract));
        when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Contract updated = contractService.updateContractProgress(5L, Map.of(
                "progressPercentage", 50,
                "lastActivityDate", "2026-03-15"
        ));

        assertEquals("keep", updated.getMetadata().get("existingKey"));
        assertEquals(50, updated.getMetadata().get("progressPercentage"));
        assertEquals("2026-03-15", updated.getMetadata().get("lastActivityDate"));
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
