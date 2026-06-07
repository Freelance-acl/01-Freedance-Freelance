package com.team01.freelance.contract.controller;

import com.team01.freelance.contract.model.Contract;
import com.team01.freelance.contract.model.ContractStatus;
import com.team01.freelance.contract.repository.ContractRepository;
import com.team01.freelance.contract.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@WithMockUser(roles = "ADMIN")
class ContractAnalyticsIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ContractRepository contractRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(webApplicationContext);
    }

    @Test
    void getAnalyticsAggregatesContracts() throws Exception {
        Contract active = new Contract();
        active.setJobId(9001L);
        active.setFreelancerId(1001L);
        active.setClientId(2001L);
        active.setProposalId(3001L);
        active.setAgreedAmount(1500.0);
        active.setStatus(ContractStatus.ACTIVE);
        active.setCreatedAt(LocalDateTime.of(2026, 3, 1, 9, 0));
        active.setStartDate(LocalDateTime.of(2026, 3, 1, 9, 0));
        active.setEndDate(LocalDateTime.of(2026, 3, 4, 9, 0));
        contractRepository.save(active);

        Contract completed = new Contract();
        completed.setJobId(9002L);
        completed.setFreelancerId(1002L);
        completed.setClientId(2002L);
        completed.setProposalId(3002L);
        completed.setAgreedAmount(2500.0);
        completed.setStatus(ContractStatus.COMPLETED);
        completed.setCreatedAt(LocalDateTime.of(2026, 3, 2, 9, 0));
        completed.setStartDate(LocalDateTime.of(2026, 3, 2, 9, 0));
        completed.setEndDate(LocalDateTime.of(2026, 3, 7, 9, 0));
        contractRepository.save(completed);

        mockMvc.perform(get("/api/contracts/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalContracts").value(2))
                .andExpect(jsonPath("$.avgValue").value(2000.0))
                .andExpect(jsonPath("$.completionRate").value(50.0))
                .andExpect(jsonPath("$.avgDurationDays").value(4.0))
                .andExpect(jsonPath("$.byStatus.ACTIVE").value(1))
                .andExpect(jsonPath("$.byStatus.COMPLETED").value(1));
    }
}
