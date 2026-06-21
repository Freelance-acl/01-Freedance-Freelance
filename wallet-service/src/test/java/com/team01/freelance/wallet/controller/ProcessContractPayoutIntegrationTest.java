package com.team01.freelance.wallet.controller;

import com.team01.freelance.contract.model.Contract;
import com.team01.freelance.contract.model.ContractStatus;
import com.team01.freelance.contract.repository.ContractRepository;
import com.team01.freelance.user.model.User;
import com.team01.freelance.user.model.UserRole;
import com.team01.freelance.user.model.UserStatus;
import com.team01.freelance.user.repository.UserRepository;
import com.team01.freelance.wallet.dto.ContractDTO;
import com.team01.freelance.wallet.feign.ContractServiceClient;
import com.team01.freelance.wallet.model.Payout;
import com.team01.freelance.wallet.model.PayoutMethod;
import com.team01.freelance.wallet.model.PayoutStatus;
import com.team01.freelance.wallet.repository.PayoutRepository;
import com.team01.freelance.wallet.service.PayoutService;
import com.team01.freelance.wallet.support.AbstractIntegrationTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [S5-F4] Integration tests for {@code POST /api/payouts/contract/{contractId}}.
 */
@Transactional
@WithMockUser(roles = "ADMIN")
class ProcessContractPayoutIntegrationTest extends AbstractIntegrationTest {

    private static final String PROCESS_URL = "/api/payouts/contract/{contractId}";

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private PayoutRepository payoutRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PayoutService payoutService;

    private User freelancer;
    private User client;

    private Long activeContractId = -1L;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(webApplicationContext);

        client = saveUser("Client", UserRole.CLIENT);
        freelancer = saveUser("Freelancer", UserRole.FREELANCER);

        // Injecting a manual stub to bypass Feign entirely without Mockito
        ContractServiceClient stubClient = new ContractServiceClient() {
            @Override
            public String getContractStatus(Long contractId) {
                return contractId.equals(activeContractId) ? "ACTIVE" : "COMPLETED";
            }

            @Override
            public ContractDTO getContract(Long contractId) {
                if (contractId == 999_999L) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contract not found");
                }
                ContractDTO dto = new ContractDTO();
                dto.setId(contractId);
                dto.setStatus(contractId.equals(activeContractId) ? "ACTIVE" : "COMPLETED");
                dto.setClientId(client.getId());
                dto.setAgreedAmount(3000.0);
                dto.setProposalId(1L);
                return dto;
            }
        };
        ReflectionTestUtils.setField(payoutService, "contractServiceClient", stubClient);
    }

    @Test
    void processCompletedContract_returns201AndUpdatesPendingPayout() throws Exception {
        Contract contract = saveContract(ContractStatus.COMPLETED, 3000.0);
        Payout pending = savePendingPayout(contract.getId(), freelancer.getId(), 3000.0);

        mockMvc.perform(post(PROCESS_URL, contract.getId())
                        .header("X-User-Id", String.valueOf(client.getId()))
                        .header("X-User-Role", "CLIENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"method":"BANK_TRANSFER","accountLastFour":"9876"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(pending.getId().intValue()))
                .andExpect(jsonPath("$.contractId").value(contract.getId().intValue()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.method").value("BANK_TRANSFER"))
                .andExpect(jsonPath("$.amount").value(3000.0))
                .andExpect(jsonPath("$.transactionDetails.method").value("BANK_TRANSFER"))
                .andExpect(jsonPath("$.transactionDetails.accountLastFour").value("9876"))
                .andExpect(jsonPath("$.transactionDetails.transactionId", notNullValue()))
                .andExpect(jsonPath("$.transactionDetails.processedAt", notNullValue()));

        Payout updated = payoutRepository.findById(pending.getId()).orElseThrow();
        assertEquals(PayoutStatus.COMPLETED, updated.getStatus());
        assertEquals(PayoutMethod.BANK_TRANSFER, updated.getMethod());
        assertEquals("BANK_TRANSFER", updated.getTransactionDetails().get("method"));
        assertEquals("9876", updated.getTransactionDetails().get("accountLastFour"));
        assertNotNull(updated.getTransactionDetails().get("transactionId"));
        assertNotNull(updated.getTransactionDetails().get("processedAt"));
    }

    @Test
    void processTwice_secondRequestReturns200Idempotent() throws Exception {
        Contract contract = saveContract(ContractStatus.COMPLETED, 3000.0);
        savePendingPayout(contract.getId(), freelancer.getId(), 3000.0);

        String body = """
                {"method":"BANK_TRANSFER","accountLastFour":"9876"}
                """;

        // First execution acts normally -> 201
        mockMvc.perform(post(PROCESS_URL, contract.getId())
                        .header("X-User-Id", String.valueOf(client.getId()))
                        .header("X-User-Role", "CLIENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        // Idempotent execution triggers check -> 200
        mockMvc.perform(post(PROCESS_URL, contract.getId())
                        .header("X-User-Id", String.valueOf(client.getId()))
                        .header("X-User-Role", "CLIENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void processActiveContract_returns400() throws Exception {
        Contract contract = saveContract(ContractStatus.ACTIVE, 3000.0);
        savePendingPayout(contract.getId(), freelancer.getId(), 3000.0);

        // Let the stub know to return ACTIVE for this ID
        this.activeContractId = contract.getId();

        mockMvc.perform(post(PROCESS_URL, contract.getId())
                        .header("X-User-Id", String.valueOf(client.getId()))
                        .header("X-User-Role", "CLIENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"method":"BANK_TRANSFER","accountLastFour":"9876"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("not completed")));
    }

    @Test
    void processUnknownContract_returns404() throws Exception {
        mockMvc.perform(post(PROCESS_URL, 999_999L)
                        .header("X-User-Id", String.valueOf(client.getId()))
                        .header("X-User-Role", "CLIENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"method":"BANK_TRANSFER"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Contract not found")));
    }

    @Test
    void processCompletedContractWithoutPendingPayout_returns404() throws Exception {
        Contract contract = saveContract(ContractStatus.COMPLETED, 3000.0);

        mockMvc.perform(post(PROCESS_URL, contract.getId())
                        .header("X-User-Id", String.valueOf(client.getId()))
                        .header("X-User-Role", "CLIENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"method":"BANK_TRANSFER"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("No pending payout")));
    }

    @Test
    void processContractPayout_withDifferentClient_returns403() throws Exception {
        Contract contract = saveContract(ContractStatus.COMPLETED, 3000.0);
        savePendingPayout(contract.getId(), freelancer.getId(), 3000.0);

        mockMvc.perform(post(PROCESS_URL, contract.getId())
                        .header("X-User-Id", "999") // Unauthorized client
                        .header("X-User-Role", "CLIENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"method":"BANK_TRANSFER","accountLastFour":"9876"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("Only the contract's client (or an ADMIN) can release this payout")));
    }

    @Test
    void processContractPayout_withAdmin_returns201() throws Exception {
        Contract contract = saveContract(ContractStatus.COMPLETED, 3000.0);
        savePendingPayout(contract.getId(), freelancer.getId(), 3000.0);

        mockMvc.perform(post(PROCESS_URL, contract.getId())
                        .header("X-User-Id", "999")
                        .header("X-User-Role", "ADMIN") // Admin role overrides client check
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"method":"BANK_TRANSFER","accountLastFour":"9876"}
                                """))
                .andExpect(status().isCreated());
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

    private Contract saveContract(ContractStatus status, double agreedAmount) {
        Contract contract = new Contract();
        contract.setJobId(1L);
        contract.setProposalId(1L);
        contract.setClientId(client.getId());
        contract.setFreelancerId(freelancer.getId());
        contract.setAgreedAmount(agreedAmount);
        contract.setStatus(status);
        contract.setStartDate(LocalDateTime.now().minusDays(7));
        if (status == ContractStatus.COMPLETED) {
            contract.setEndDate(LocalDateTime.now());
        }
        return contractRepository.save(contract);
    }

    private Payout savePendingPayout(Long contractId, Long freelancerId, double amount) {
        Payout payout = new Payout();
        payout.setContractId(contractId);
        payout.setFreelancerId(freelancerId);
        payout.setAmount(amount);
        payout.setMethod(PayoutMethod.BANK_TRANSFER);
        payout.setStatus(PayoutStatus.PENDING);
        return payoutRepository.save(payout);
    }
}