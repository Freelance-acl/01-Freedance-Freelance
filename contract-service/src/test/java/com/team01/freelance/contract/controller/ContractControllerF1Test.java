package com.team01.freelance.contract.controller;

import com.team01.freelance.contract.model.Contract;
import com.team01.freelance.contract.service.ContractService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ContractControllerF1Test {

    private MockMvc mockMvc;
    private ContractService contractService;

    @BeforeEach
    void setUp() {
        ContractController controller = new ContractController();
        contractService = mock(ContractService.class);
        ReflectionTestUtils.setField(controller, "contractService", contractService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getActiveContractReturnsOk() throws Exception {
        when(contractService.getActiveContractForUser(1L)).thenReturn(new Contract());

        mockMvc.perform(get("/api/contracts/user/{userId}/active", 1L))
                .andExpect(status().isOk());
    }

    @Test
    void getActiveContractReturnsNotFound() throws Exception {
        when(contractService.getActiveContractForUser(999L)).thenThrow(new EntityNotFoundException("missing"));

        mockMvc.perform(get("/api/contracts/user/{userId}/active", 999L))
                .andExpect(status().isNotFound());
    }
}
