package com.team01.freelance.wallet.controller;

import com.team01.freelance.contract.model.Contract;
import com.team01.freelance.contract.model.ContractStatus;
import com.team01.freelance.contract.repository.ContractRepository;
import com.team01.freelance.user.model.User;
import com.team01.freelance.user.model.UserRole;
import com.team01.freelance.user.model.UserStatus;
import com.team01.freelance.user.repository.UserRepository;
import com.team01.freelance.wallet.model.Payout;
import com.team01.freelance.wallet.model.PayoutMethod;
import com.team01.freelance.wallet.model.PayoutStatus;
import com.team01.freelance.wallet.repository.PayoutRepository;
import com.team01.freelance.wallet.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [S5-F6] Integration tests for {@code GET /api/payouts/reports/revenue}.
 */
@Transactional
class RevenueReportIntegrationTest extends AbstractIntegrationTest {

    private static final String REVENUE_URL = "/api/payouts/reports/revenue";

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private PayoutRepository payoutRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private UserRepository userRepository;

    private User freelancer;
    private Long contractId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        User client = saveUser("Client", UserRole.CLIENT);
        freelancer = saveUser("Freelancer", UserRole.FREELANCER);

        Contract contract = new Contract();
        contract.setJobId(1L);
        contract.setProposalId(1L);
        contract.setClientId(client.getId());
        contract.setFreelancerId(freelancer.getId());
        contract.setAgreedAmount(1000.0);
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setStartDate(LocalDateTime.now().minusDays(30));
        contractId = contractRepository.save(contract).getId();
    }

    /**
     * Spec (a)+(b): March COMPLETED + REFUNDED payouts → aggregated revenue metrics.
     */
    @Test
    void getRevenueReport_marchRange_returnsExpectedMetrics() throws Exception {
        savePayout(PayoutStatus.COMPLETED, 1000.0, LocalDateTime.of(2026, 3, 5, 10, 0));
        savePayout(PayoutStatus.COMPLETED, 1500.0, LocalDateTime.of(2026, 3, 8, 10, 0));
        savePayout(PayoutStatus.COMPLETED, 2000.0, LocalDateTime.of(2026, 3, 12, 10, 0));
        savePayout(PayoutStatus.COMPLETED, 2500.0, LocalDateTime.of(2026, 3, 18, 10, 0));
        savePayout(PayoutStatus.COMPLETED, 3000.0, LocalDateTime.of(2026, 3, 25, 10, 0));
        savePayout(PayoutStatus.REFUNDED, 800.0, LocalDateTime.of(2026, 3, 10, 10, 0));
        savePayout(PayoutStatus.REFUNDED, 1200.0, LocalDateTime.of(2026, 3, 20, 10, 0));

        // Outside range — must not be included
        savePayout(PayoutStatus.COMPLETED, 5000.0, LocalDateTime.of(2026, 2, 28, 10, 0));
        savePayout(PayoutStatus.COMPLETED, 5000.0, LocalDateTime.of(2026, 4, 1, 10, 0));

        mockMvc.perform(get(REVENUE_URL)
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRevenue").value(10000.0))
                .andExpect(jsonPath("$.totalTransactions").value(5))
                .andExpect(jsonPath("$.averagePayout").value(2000.0))
                .andExpect(jsonPath("$.refundedAmount").value(2000.0))
                .andExpect(jsonPath("$.refundCount").value(2));
    }

    /** Spec (c): startDate after endDate → 400. */
    @Test
    void getRevenueReport_startDateAfterEndDate_returns400() throws Exception {
        mockMvc.perform(get(REVENUE_URL)
                        .param("startDate", "2026-03-31")
                        .param("endDate", "2026-03-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("startDate")));
    }

    @Test
    void getRevenueReport_emptyRange_returnsZeroes() throws Exception {
        mockMvc.perform(get(REVENUE_URL)
                        .param("startDate", "2026-06-01")
                        .param("endDate", "2026-06-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRevenue").value(0.0))
                .andExpect(jsonPath("$.totalTransactions").value(0))
                .andExpect(jsonPath("$.averagePayout").value(0.0))
                .andExpect(jsonPath("$.refundedAmount").value(0.0))
                .andExpect(jsonPath("$.refundCount").value(0));
    }

    private User saveUser(String prefix, UserRole role) {
        User user = new User();
        user.setName(prefix);
        user.setEmail(prefix.toLowerCase() + "-" + System.nanoTime() + "@test.dev");
        user.setPassword("secret");
        user.setPhone("+1" + (System.nanoTime() % 1_000_000_000L));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    private void savePayout(PayoutStatus status, double amount, LocalDateTime createdAt) {
        Payout payout = new Payout();
        payout.setContractId(contractId);
        payout.setFreelancerId(freelancer.getId());
        payout.setAmount(amount);
        payout.setMethod(PayoutMethod.BANK_TRANSFER);
        payout.setStatus(status);
        payout.setCreatedAt(createdAt);
        payoutRepository.save(payout);
    }
}
