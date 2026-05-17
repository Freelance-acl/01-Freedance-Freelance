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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [S5-F1] Integration tests for {@code GET /api/payouts/search}.
 */
@Transactional
class PayoutSearchIntegrationTest extends AbstractIntegrationTest {

    private static final String SEARCH_URL = "/api/payouts/search";

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
    void search_completedInMarch_returnsTwoMostRecentFirst() throws Exception {
        savePayout(PayoutStatus.COMPLETED, 100.0, LocalDateTime.of(2026, 3, 5, 10, 0));
        savePayout(PayoutStatus.COMPLETED, 200.0, LocalDateTime.of(2026, 3, 20, 10, 0));
        savePayout(PayoutStatus.REFUNDED, 50.0, LocalDateTime.of(2026, 3, 10, 10, 0));
        savePayout(PayoutStatus.COMPLETED, 300.0, LocalDateTime.of(2026, 2, 15, 10, 0));

        mockMvc.perform(get(SEARCH_URL)
                        .param("status", "COMPLETED")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].amount").value(200.0))
                .andExpect(jsonPath("$[1].amount").value(100.0));
    }

    @Test
    void search_marchWithoutStatus_returnsThree() throws Exception {
        savePayout(PayoutStatus.COMPLETED, 100.0, LocalDateTime.of(2026, 3, 5, 10, 0));
        savePayout(PayoutStatus.REFUNDED, 50.0, LocalDateTime.of(2026, 3, 10, 10, 0));
        savePayout(PayoutStatus.COMPLETED, 300.0, LocalDateTime.of(2026, 2, 15, 10, 0));

        mockMvc.perform(get(SEARCH_URL)
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
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
