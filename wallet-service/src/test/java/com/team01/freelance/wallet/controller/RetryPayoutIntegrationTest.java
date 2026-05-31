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
import org.springframework.security.test.context.support.WithMockUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [S5-F7] Integration tests for {@code PUT /api/payouts/{id}/retry}.
 */
@Transactional
@WithMockUser(roles = "ADMIN")
class RetryPayoutIntegrationTest extends AbstractIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private PayoutRepository payoutRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private UserRepository userRepository;

    private Long freelancerId;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(webApplicationContext);
        User client = saveUser("Client", UserRole.CLIENT);
        User freelancer = saveUser("Freelancer", UserRole.FREELANCER);
        freelancerId = freelancer.getId();

        Contract contract = new Contract();
        contract.setJobId(1L);
        contract.setProposalId(1L);
        contract.setClientId(client.getId());
        contract.setFreelancerId(freelancerId);
        contract.setAgreedAmount(1000.0);
        contract.setStatus(ContractStatus.COMPLETED);
        contract.setStartDate(LocalDateTime.now().minusDays(10));
        contractRepository.save(contract);
    }

    @Test
    void retryFailedPayout_setsCompletedAndIncrementsRetryAttempt() throws Exception {
        Payout payout = saveFailedPayout();

        mockMvc.perform(put("/api/payouts/{id}/retry", payout.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        Payout updated = payoutRepository.findById(payout.getId()).orElseThrow();
        assertEquals(PayoutStatus.COMPLETED, updated.getStatus());
        assertEquals(1, ((Number) updated.getTransactionDetails().get("retryAttempt")).intValue());
    }

    private Payout saveFailedPayout() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("retryAttempt", 0);
        details.put("gatewayResponse", "declined");

        Payout payout = new Payout();
        payout.setContractId(contractRepository.findAll().get(0).getId());
        payout.setFreelancerId(freelancerId);
        payout.setAmount(1000.0);
        payout.setMethod(PayoutMethod.BANK_TRANSFER);
        payout.setStatus(PayoutStatus.FAILED);
        payout.setTransactionDetails(details);
        return payoutRepository.save(payout);
    }

    private User saveUser(String name, UserRole role) {
        User user = new User();
        user.setName(name);
        user.setEmail("retry-" + System.nanoTime() + "@test.dev");
        user.setPassword("secret");
        user.setPhone("+9000" + (System.nanoTime() % 1_000_000_000L));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }
}