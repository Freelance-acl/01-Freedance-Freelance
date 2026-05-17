package com.team01.freelance.contract.controller;

import com.team01.freelance.contract.model.Contract;
import com.team01.freelance.contract.service.ContractService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ContractControllerF2Test {

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
    void updateProgressReturnsOk() throws Exception {
        when(contractService.updateContractProgress(eq(7L), any())).thenReturn(new Contract());

        mockMvc.perform(put("/api/contracts/{contractId}/progress", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"progressPercentage\":50}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateProgressReturnsNotFound() throws Exception {
        when(contractService.updateContractProgress(eq(77L), any(Map.class)))
                .thenThrow(new EntityNotFoundException("missing"));

        mockMvc.perform(put("/api/contracts/{contractId}/progress", 77L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"progressPercentage\":50}"))
                .andExpect(status().isNotFound());
    }
}
