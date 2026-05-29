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
import com.team01.freelance.wallet.support.TestPayoutEventObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [S5-F12] §10.5.3 — milestone-based payout reversal scenarios (a–g).
 */
@Transactional
class MilestoneReversalIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private PayoutRepository payoutRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestPayoutEventObserver testPayoutEventObserver;

    private MockMvc mockMvc;
    private Long freelancerId;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(webApplicationContext);
        testPayoutEventObserver.reset();

        User client = saveUser("Client", UserRole.CLIENT);
        User freelancer = saveUser("Freelancer", UserRole.FREELANCER);
        freelancerId = freelancer.getId();
        contractRepository.save(buildContract(client.getId(), freelancerId, 99L));
    }

    // (a) MILESTONE_ONLY — incomplete milestones only (2000 of 5000 payout)
    @Test
    @WithMockUser(roles = "CLIENT")
    void reverseMilestone_milestoneOnly_incompleteMilestonesOnly() throws Exception {
        Long proposalId = 1001L;
        Long contractId = saveContractWithProposal(proposalId);
        insertMilestones(proposalId, "COMPLETED", 1500.0, "COMPLETED", 1500.0, "IN_PROGRESS", 2000.0);

        Payout payout = savePayout(contractId, 5000.0, PayoutStatus.COMPLETED, LocalDateTime.now());

        mockMvc.perform(post("/api/payouts/{id}/reverse-milestone", payout.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"milestone dispute","reversalScope":"MILESTONE_ONLY"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"))
                .andExpect(jsonPath("$.transactionDetails.refundAmount").value(2000.0))
                .andExpect(jsonPath("$.transactionDetails.reversalScope").value("MILESTONE_ONLY"));

        Payout updated = payoutRepository.findById(payout.getId()).orElseThrow();
        assertEquals(PayoutStatus.REFUNDED, updated.getStatus());
        assertEquals(2000.0, updated.getTransactionDetails().get("refundAmount"));
        assertTrue(testPayoutEventObserver.hasAuditAction(payout.getId(), "REFUNDED"));
    }

    // (b) FULL reversal within window
    @Test
    @WithMockUser(roles = "CLIENT")
    void reverseMilestone_fullScope_refundsEntirePayout() throws Exception {
        Long contractId = saveContractWithProposal(1002L);
        Payout payout = savePayout(contractId, 3000.0, PayoutStatus.COMPLETED, LocalDateTime.now());

        mockMvc.perform(post("/api/payouts/{id}/reverse-milestone", payout.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"full rollback","reversalScope":"FULL"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"))
                .andExpect(jsonPath("$.transactionDetails.refundAmount").value(3000.0))
                .andExpect(jsonPath("$.transactionDetails.reversalScope").value("FULL"));
    }

    // (c) Outside 30-day window — 400, REFUND_DENIED audit, cache invalidation
    @Test
    @WithMockUser(roles = "CLIENT")
    void reverseMilestone_outsideWindow_returns400WithAuditAndCacheInvalidation() throws Exception {
        Long contractId = saveContractWithProposal(1003L);
        Payout payout = savePayout(contractId, 1000.0, PayoutStatus.COMPLETED, LocalDateTime.now().minusDays(40));

        mockMvc.perform(post("/api/payouts/{id}/reverse-milestone", payout.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"too late","reversalScope":"FULL"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("reversal window expired"));

        Payout unchanged = payoutRepository.findById(payout.getId()).orElseThrow();
        assertEquals(PayoutStatus.COMPLETED, unchanged.getStatus());
        assertTrue(testPayoutEventObserver.hasAuditAction(payout.getId(), "REFUND_DENIED"));
        assertTrue(testPayoutEventObserver.isWalletAnalyticsInvalidated());
    }

    // (d) PENDING payout → 400
    @Test
    @WithMockUser(roles = "CLIENT")
    void reverseMilestone_pendingPayout_returns400() throws Exception {
        Long contractId = saveContractWithProposal(1004L);
        Payout payout = savePayout(contractId, 1000.0, PayoutStatus.PENDING, LocalDateTime.now());

        mockMvc.perform(post("/api/payouts/{id}/reverse-milestone", payout.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"n/a","reversalScope":"FULL"}
                                """))
                .andExpect(status().isBadRequest());
    }

    // (e) Already REFUNDED → 400
    @Test
    @WithMockUser(roles = "CLIENT")
    void reverseMilestone_alreadyRefunded_returns400() throws Exception {
        Long contractId = saveContractWithProposal(1005L);
        Payout payout = savePayout(contractId, 1000.0, PayoutStatus.REFUNDED, LocalDateTime.now());

        mockMvc.perform(post("/api/payouts/{id}/reverse-milestone", payout.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"again","reversalScope":"FULL"}
                                """))
                .andExpect(status().isBadRequest());
    }

    // (f) Unknown payout → 404
    @Test
    @WithMockUser(roles = "CLIENT")
    void reverseMilestone_unknownPayout_returns404() throws Exception {
        mockMvc.perform(post("/api/payouts/{id}/reverse-milestone", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"n/a","reversalScope":"FULL"}
                                """))
                .andExpect(status().isNotFound());
    }

    // (g) No JWT → 401
    @Test
    void reverseMilestone_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/payouts/{id}/reverse-milestone", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"n/a","reversalScope":"FULL"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    private Long saveContractWithProposal(Long proposalId) {
        User client = saveUser("C-" + proposalId, UserRole.CLIENT);
        User freelancer = saveUser("F-" + proposalId, UserRole.FREELANCER);
        Contract contract = buildContract(client.getId(), freelancer.getId(), proposalId);
        return contractRepository.save(contract).getId();
    }

    private Contract buildContract(Long clientId, Long freelancerId, Long proposalId) {
        Contract contract = new Contract();
        contract.setJobId(1L);
        contract.setProposalId(proposalId);
        contract.setClientId(clientId);
        contract.setFreelancerId(freelancerId);
        contract.setAgreedAmount(10000.0);
        contract.setStatus(ContractStatus.COMPLETED);
        contract.setStartDate(LocalDateTime.now().minusDays(5));
        return contract;
    }

    private void insertMilestones(
            Long proposalId,
            String status1, double amount1,
            String status2, double amount2,
            String status3, double amount3) {
        jdbcTemplate.update(
                """
                INSERT INTO proposal_milestones
                    (proposal_id, milestone_order, title, description, amount, status)
                VALUES (?, 1, 'M1', 'Phase 1', ?, ?)
                """,
                proposalId, amount1, status1);
        jdbcTemplate.update(
                """
                INSERT INTO proposal_milestones
                    (proposal_id, milestone_order, title, description, amount, status)
                VALUES (?, 2, 'M2', 'Phase 2', ?, ?)
                """,
                proposalId, amount2, status2);
        jdbcTemplate.update(
                """
                INSERT INTO proposal_milestones
                    (proposal_id, milestone_order, title, description, amount, status)
                VALUES (?, 3, 'M3', 'Phase 3', ?, ?)
                """,
                proposalId, amount3, status3);
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

    private Payout savePayout(Long contractId, double amount, PayoutStatus status, LocalDateTime createdAt) {
        Payout payout = new Payout();
        payout.setContractId(contractId);
        payout.setFreelancerId(freelancerId);
        payout.setAmount(amount);
        payout.setMethod(PayoutMethod.BANK_TRANSFER);
        payout.setStatus(status);
        payout.setTransactionDetails(Map.of("gatewayResponse", "approved"));
        payout.setCreatedAt(createdAt);
        return payoutRepository.save(payout);
    }
}
