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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [S5-F3] Integration tests for {@code GET /api/payouts/freelancer/{freelancerId}/summary}.
 */
@Transactional
class FreelancerPayoutSummaryIntegrationTest extends AbstractIntegrationTest {

    private static final String SUMMARY_URL = "/api/payouts/freelancer/{freelancerId}/summary";

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

    @Test
    void getFreelancerSummary_returnsBreakdownAndTotals() throws Exception {
        saveCompletedPayout(1500.0, PayoutMethod.BANK_TRANSFER);
        saveCompletedPayout(2000.0, PayoutMethod.BANK_TRANSFER);
        saveCompletedPayout(800.0, PayoutMethod.PAYPAL);
        saveCompletedPayout(500.0, PayoutMethod.CRYPTO);
        saveCompletedPayout(999.0, PayoutStatus.PENDING, PayoutMethod.BANK_TRANSFER);

        mockMvc.perform(get(SUMMARY_URL, freelancer.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.freelancerId").value(freelancer.getId().intValue()))
                .andExpect(jsonPath("$.totalPayouts").value(4))
                .andExpect(jsonPath("$.totalAmount").value(4800.0))
                .andExpect(jsonPath("$.methodBreakdown.BANK_TRANSFER").value(3500.0))
                .andExpect(jsonPath("$.methodBreakdown.PAYPAL").value(800.0))
                .andExpect(jsonPath("$.methodBreakdown.CRYPTO").value(500.0));
    }

    @Test
    void getFreelancerSummary_unknownUser_returns404() throws Exception {
        mockMvc.perform(get(SUMMARY_URL, 999_999L))
                .andExpect(status().isNotFound());
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

    private void saveCompletedPayout(double amount, PayoutMethod method) {
        saveCompletedPayout(amount, PayoutStatus.COMPLETED, method);
    }

    private void saveCompletedPayout(double amount, PayoutStatus status, PayoutMethod method) {
        Payout payout = new Payout();
        payout.setContractId(contractId);
        payout.setFreelancerId(freelancer.getId());
        payout.setAmount(amount);
        payout.setMethod(method);
        payout.setStatus(status);
        payout.setCreatedAt(LocalDateTime.now());
        payoutRepository.save(payout);
    }
}
