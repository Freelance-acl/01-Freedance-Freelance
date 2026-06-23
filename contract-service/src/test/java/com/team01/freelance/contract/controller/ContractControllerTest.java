package com.team01.freelance.contract.controller;

import com.team01.freelance.contract.dto.FreelancerPerformanceDTO;
import com.team01.freelance.contract.dto.StalledContractDTO;
import com.team01.freelance.contract.model.Contract;
import com.team01.freelance.contract.service.ContractService;
import com.team01.freelance.user.dto.UserContractSummaryDTO;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class ContractControllerTest {

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
    void getAllReturnsOk() throws Exception {
        when(contractService.getAllContracts()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/contracts"))
                .andExpect(status().isOk());
    }

    @Test
    void getByIdReturnsOk() throws Exception {
        Contract contract = new Contract();
        when(contractService.getContractById(1L)).thenReturn(Optional.of(contract));

        mockMvc.perform(get("/api/contracts/{id}", 1L))
                .andExpect(status().isOk());
    }

    @Test
    void createReturnsOk() throws Exception {
        Contract contract = new Contract();
        when(contractService.createContract(any(Contract.class))).thenReturn(contract);

        mockMvc.perform(post("/api/contracts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateReturnsOk() throws Exception {
        Contract contract = new Contract();
        when(contractService.updateContract(eq(1L), any(Contract.class))).thenReturn(contract);

        mockMvc.perform(put("/api/contracts/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateReturnsNotFound() throws Exception {
        when(contractService.updateContract(eq(1L), any(Contract.class))).thenThrow(new RuntimeException("Not found"));

        mockMvc.perform(put("/api/contracts/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteByIdReturnsNoContent() throws Exception {
        when(contractService.deleteContractById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/contracts/{id}", 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteAllReturnsNoContent() throws Exception {
        doNothing().when(contractService).deleteAllContracts();

        mockMvc.perform(delete("/api/contracts/all"))
                .andExpect(status().isNoContent());
    }

    @Test
    void purgeReturnsOk() throws Exception {
        when(contractService.purgeOldContractData(30)).thenReturn(7L);

        mockMvc.perform(delete("/api/contracts/purge").param("olderThanDays", "30"))
                .andExpect(status().isOk());
    }

    @Test
    void summaryReturnsOk() throws Exception {
        FreelancerPerformanceDTO dto = new FreelancerPerformanceDTO(10L, 5L, 1400.0, 80.0, 17.5, 7000.0);
        when(contractService.getFreelancerPerformanceSummary(eq(10L), any(), any())).thenReturn(dto);

        mockMvc.perform(get("/api/contracts/freelancer/{freelancerId}/summary", 10L)
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31"))
                .andExpect(status().isOk());
    }

    @Test
    void summaryReturnsNotFound() throws Exception {
        when(contractService.getFreelancerPerformanceSummary(eq(999L), any(), any()))
                .thenThrow(new EntityNotFoundException("not found"));

        mockMvc.perform(get("/api/contracts/freelancer/{freelancerId}/summary", 999L)
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31"))
                .andExpect(status().isNotFound());
    }

    @Test
    void stalledReturnsOk() throws Exception {
        when(contractService.findStalledContracts(50.0, 7))
                .thenReturn(List.of(new StalledContractDTO(1L, "A", "B", 1000.0, 10.0, 30L)));

        mockMvc.perform(get("/api/contracts/stalled")
                        .param("maxProgress", "50")
                        .param("stalledDays", "7"))
                .andExpect(status().isOk());
    }

    @Test
    void providerUserSummaryReturnsOk() throws Exception {
        UserContractSummaryDTO summary = UserContractSummaryDTO.builder()
                .userId(10L)
                .name("Freelancer")
                .totalContracts(3L)
                .completedContracts(2L)
                .terminatedContracts(1L)
                .totalEarnings(2500.0)
                .averageContractValue(833.3333333333334)
                .build();
        when(contractService.getUserContractSummary(10L)).thenReturn(summary);

        mockMvc.perform(get("/api/contracts/user/{id}/summary", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(10))
                .andExpect(jsonPath("$.completedContracts").value(2));
    }

    @Test
    void providerUserActiveCountReturnsOk() throws Exception {
        when(contractService.getActiveContractCountForUser(10L)).thenReturn(2);

        mockMvc.perform(get("/api/contracts/user/{id}/active-count", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(2));
    }

    @Test
    void providerUserCompletedCountReturnsOk() throws Exception {
        when(contractService.getCompletedContractCountForUser(10L)).thenReturn(4L);

        mockMvc.perform(get("/api/contracts/user/{id}/completed-count", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(4));
    }

    @Test
    void providerJobActiveCountReturnsOk() throws Exception {
        when(contractService.getActiveContractCountForJob(20L)).thenReturn(1);

        mockMvc.perform(get("/api/contracts/job/{jobId}/active-count", 20L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(1));
    }

    @Test
    void providerProposalActiveReturns404WhenNoneExists() throws Exception {
        when(contractService.getActiveContractForProposal(30L))
                .thenThrow(new EntityNotFoundException("not found"));

        mockMvc.perform(get("/api/contracts/proposal/{proposalId}/active", 30L))
                .andExpect(status().isNotFound());
    }
}
