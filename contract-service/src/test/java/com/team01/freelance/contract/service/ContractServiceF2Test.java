package com.team01.freelance.contract.service;

import com.team01.freelance.contract.model.Contract;
import com.team01.freelance.contract.repository.ContractRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContractServiceF2Test {

    private ContractService contractService;
    private ContractRepository contractRepository;

    @BeforeEach
    void setUp() {
        contractService = new ContractService();
        contractRepository = mock(ContractRepository.class);
        ReflectionTestUtils.setField(contractService, "contractRepository", contractRepository);
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
}
