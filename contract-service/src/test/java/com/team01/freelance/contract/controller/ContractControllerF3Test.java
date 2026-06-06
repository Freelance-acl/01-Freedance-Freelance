package com.team01.freelance.contract.controller;

import com.team01.freelance.contract.dto.ContractSummaryDTO;
import com.team01.freelance.contract.service.ContractService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ContractControllerF3Test {

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
