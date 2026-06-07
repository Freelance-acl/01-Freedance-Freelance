package com.team01.freelance.contract.controller;

import com.team01.freelance.contract.dto.ContractSummaryDTO;
import com.team01.freelance.contract.model.Contract;
import com.team01.freelance.contract.service.ContractService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ContractControllerF123Test {

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

    @Test
    void searchContractsReturnsOk() throws Exception {
        when(contractService.searchContracts(500.0, 4000.0, null))
                .thenReturn(List.of(new ContractSummaryDTO()));

        mockMvc.perform(get("/api/contracts/search")
                        .param("minAmount", "500")
                        .param("maxAmount", "4000"))
                .andExpect(status().isOk());
    }

    @Test
    void searchContractsReturnsBadRequest() throws Exception {
        when(contractService.searchContracts(4000.0, 500.0, "ACTIVE"))
                .thenThrow(new IllegalArgumentException("bad range"));

        mockMvc.perform(get("/api/contracts/search")
                        .param("minAmount", "4000")
                        .param("maxAmount", "500")
                        .param("status", "ACTIVE"))
                .andExpect(status().isBadRequest());
    }
}
