package com.team01.freelance.contract.controller;

import com.team01.freelance.contract.model.Contract;
import com.team01.freelance.contract.model.ContractStatus;
import com.team01.freelance.contract.repository.ContractRepository;
import com.team01.freelance.contract.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.test.context.support.WithMockUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@WithMockUser(roles = "ADMIN")
class ContractF2IntegrationTest extends AbstractIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ContractRepository contractRepository;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(webApplicationContext);
    }

    @Test
    void updateContractProgressMergesIncomingMetadata() throws Exception {
        Contract contract = new Contract();
        contract.setJobId(401L);
        contract.setFreelancerId(501L);
        contract.setClientId(601L);
        contract.setProposalId(1401L);
        contract.setAgreedAmount(1500.0);
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setCreatedAt(LocalDateTime.of(2026, 3, 1, 9, 0));
        contract.setStartDate(LocalDateTime.of(2026, 3, 7, 9, 0));
        contract.setMetadata(new LinkedHashMap<>(Map.of(
                "existingKey", "keep",
                "progressPercentage", 10
        )));
        contract = contractRepository.save(contract);

        mockMvc.perform(put("/api/contracts/{contractId}/progress", contract.getId())
                        .contentType("application/json")
                        .content("{\"progressPercentage\":50,\"lastActivityDate\":\"2026-03-15\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata.existingKey").value("keep"))
                .andExpect(jsonPath("$.metadata.progressPercentage").value(50))
                .andExpect(jsonPath("$.metadata.lastActivityDate").value("2026-03-15"));
    }

    @Test
    void updateContractProgressUnknownContractReturns404() throws Exception {
        mockMvc.perform(put("/api/contracts/{contractId}/progress", 999999L)
                        .contentType("application/json")
                        .content("{\"progressPercentage\":50}"))
                .andExpect(status().isNotFound());
    }
}