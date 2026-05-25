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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [S5-F2] Integration tests for {@code PUT /api/payouts/{id}/refund}.
 */
@Transactional
@WithMockUser(roles = "ADMIN")
class RefundPayoutIntegrationTest extends AbstractIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private PayoutRepository payoutRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private UserRepository userRepository;

    private Long contractId;
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
        contract.setStartDate(LocalDateTime.now().minusDays(30));
        contractId = contractRepository.save(contract).getId();
    }

    @Test
    void refund_completedPayout_setsRefundedAndMetadata() throws Exception {
        Payout payout = savePayout(PayoutStatus.COMPLETED, Map.of("gatewayResponse", "approved"));

        mockMvc.perform(put("/api/payouts/{id}/refund", payout.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"contract terminated early\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));

        Payout updated = payoutRepository.findById(payout.getId()).orElseThrow();
        assertEquals(PayoutStatus.REFUNDED, updated.getStatus());
        assertEquals("contract terminated early", updated.getTransactionDetails().get("refundReason"));
        assertNotNull(updated.getTransactionDetails().get("refundedAt"));
    }

    @Test
    void refund_alreadyRefunded_returns400() throws Exception {
        Payout payout = savePayout(PayoutStatus.REFUNDED, Map.of());

        mockMvc.perform(put("/api/payouts/{id}/refund", payout.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"again\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("COMPLETED")));
    }

    @Test
    void refund_failedPayout_returns400() throws Exception {
        Payout payout = savePayout(PayoutStatus.FAILED, Map.of());

        mockMvc.perform(put("/api/payouts/{id}/refund", payout.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"n/a\"}"))
                .andExpect(status().isBadRequest());
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

    private Payout savePayout(PayoutStatus status, Map<String, Object> details) {
        Payout payout = new Payout();
        payout.setContractId(contractId);
        payout.setFreelancerId(freelancerId);
        payout.setAmount(1000.0);
        payout.setMethod(PayoutMethod.BANK_TRANSFER);
        payout.setStatus(status);
        payout.setTransactionDetails(details);
        payout.setCreatedAt(LocalDateTime.now());
        return payoutRepository.save(payout);
    }
}