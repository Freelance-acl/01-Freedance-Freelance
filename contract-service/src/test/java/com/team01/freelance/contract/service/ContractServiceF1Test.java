package com.team01.freelance.contract.service;

import com.team01.freelance.contract.model.Contract;
import com.team01.freelance.contract.repository.ContractRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContractServiceF1Test {

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
}
