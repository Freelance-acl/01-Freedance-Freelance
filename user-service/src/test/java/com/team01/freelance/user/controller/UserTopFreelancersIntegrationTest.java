package com.team01.freelance.user.controller;

import com.team01.freelance.user.dto.UserContractSummaryDTO;
import com.team01.freelance.user.feign.ContractServiceClient;
import com.team01.freelance.user.feign.WalletServiceClient;
import com.team01.freelance.user.model.User;
import com.team01.freelance.user.model.UserRole;
import com.team01.freelance.user.model.UserStatus;
import com.team01.freelance.user.repository.UserRepository;
import com.team01.freelance.user.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.test.context.support.WithMockUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [S1-F6] Integration tests for {@code GET /api/users/reports/top-freelancers}.
 */
@Transactional
@WithMockUser(roles = "ADMIN")
class UserTopFreelancersIntegrationTest extends AbstractIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private WalletServiceClient walletServiceClient;

    @MockitoBean
    private ContractServiceClient contractServiceClient;

    private User freelancerA;
    private User freelancerB;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(webApplicationContext);
        freelancerA = saveFreelancer("User A");
        freelancerB = saveFreelancer("User B");
    }

    @Test
    void topFreelancers_returnsOrderedByEarnings() throws Exception {
        LocalDate startDate = LocalDate.parse("2026-03-01");
        LocalDate endDate = LocalDate.parse("2026-03-31");
        when(walletServiceClient.getFreelancerTotalEarnings(freelancerA.getId(), startDate, endDate))
                .thenReturn(new BigDecimal("4000"));
        when(walletServiceClient.getFreelancerTotalEarnings(freelancerB.getId(), startDate, endDate))
                .thenReturn(new BigDecimal("8000"));
        when(contractServiceClient.getUserContractSummary(freelancerA.getId()))
                .thenReturn(contractSummary(freelancerA, 2L));
        when(contractServiceClient.getUserContractSummary(freelancerB.getId()))
                .thenReturn(contractSummary(freelancerB, 1L));

        mockMvc.perform(get("/api/users/reports/top-freelancers")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31")
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].userId").value(freelancerB.getId()))
                .andExpect(jsonPath("$[0].name").value("User B"))
                .andExpect(jsonPath("$[0].totalEarnings").value(8000.0))
                .andExpect(jsonPath("$[0].contractCount").value(1))
                .andExpect(jsonPath("$[1].userId").value(freelancerA.getId()))
                .andExpect(jsonPath("$[1].name").value("User A"))
                .andExpect(jsonPath("$[1].totalEarnings").value(4000.0))
                .andExpect(jsonPath("$[1].contractCount").value(2));

        verify(walletServiceClient).getFreelancerTotalEarnings(freelancerA.getId(), startDate, endDate);
        verify(walletServiceClient).getFreelancerTotalEarnings(freelancerB.getId(), startDate, endDate);
        verify(contractServiceClient).getUserContractSummary(freelancerA.getId());
        verify(contractServiceClient).getUserContractSummary(freelancerB.getId());
    }

    private User saveFreelancer(String name) {
        User user = new User();
        user.setName(name);
        user.setEmail("top-" + System.nanoTime() + "@test.dev");
        user.setPassword("secret");
        user.setPhone("+7000" + (System.nanoTime() % 1_000_000_000L));
        user.setRole(UserRole.FREELANCER);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    private UserContractSummaryDTO contractSummary(User user, Long completedContracts) {
        return UserContractSummaryDTO.builder()
                .userId(user.getId())
                .name(user.getName())
                .totalContracts(completedContracts)
                .completedContracts(completedContracts)
                .terminatedContracts(0L)
                .totalEarnings(0.0)
                .averageContractValue(0.0)
                .build();
    }
}
