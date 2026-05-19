package com.team01.freelance.wallet.controller;

import com.team01.freelance.contract.model.Contract;
import com.team01.freelance.contract.model.ContractStatus;
import com.team01.freelance.contract.repository.ContractRepository;
import com.team01.freelance.user.model.User;
import com.team01.freelance.user.model.UserRole;
import com.team01.freelance.user.model.UserStatus;
import com.team01.freelance.user.repository.UserRepository;
import com.team01.freelance.wallet.model.DiscountType;
import com.team01.freelance.wallet.model.Payout;
import com.team01.freelance.wallet.model.PayoutMethod;
import com.team01.freelance.wallet.model.PayoutStatus;
import com.team01.freelance.wallet.model.PromoCode;
import com.team01.freelance.wallet.repository.PayoutRepository;
import com.team01.freelance.wallet.repository.PromoCodeRepository;
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
 * [S5-F8] Integration tests for {@code GET /api/payouts/{payoutId}/details}.
 */
@Transactional
class PayoutDetailsIntegrationTest extends AbstractIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private PayoutRepository payoutRepository;

    @Autowired
    private PromoCodeRepository promoCodeRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void payoutDetails_returnsPayoutWithAppliedPromos() throws Exception {
        User client = saveUser("Client", UserRole.CLIENT);
        User freelancer = saveUser("Freelancer", UserRole.FREELANCER);

        Contract contract = new Contract();
        contract.setJobId(1L);
        contract.setProposalId(1L);
        contract.setClientId(client.getId());
        contract.setFreelancerId(freelancer.getId());
        contract.setAgreedAmount(2000.0);
        contract.setStatus(ContractStatus.COMPLETED);
        contract.setStartDate(LocalDateTime.now().minusDays(5));
        contract = contractRepository.save(contract);

        Payout payout = new Payout();
        payout.setContractId(contract.getId());
        payout.setFreelancerId(freelancer.getId());
        payout.setAmount(2000.0);
        payout.setMethod(PayoutMethod.BANK_TRANSFER);
        payout.setStatus(PayoutStatus.PENDING);
        payout = payoutRepository.save(payout);

        PromoCode promo = new PromoCode();
        promo.setCode("SAVE10");
        promo.setDiscountType(DiscountType.PERCENTAGE);
        promo.setDiscountValue(10.0);
        promo.setMaxUses(5);
        promo.setActive(true);
        promo.setExpiryDate(LocalDateTime.now().plusDays(10));
        promo = promoCodeRepository.save(promo);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/payouts/{payoutId}/promos/{promoCodeId}", payout.getId(), promo.getId()))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/payouts/{payoutId}/details", payout.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payoutId").value(payout.getId().intValue()))
                .andExpect(jsonPath("$.originalAmount").value(2000.0))
                .andExpect(jsonPath("$.appliedPromoCodes.length()").value(1))
                .andExpect(jsonPath("$.appliedPromoCodes[0].promoCode").value("SAVE10"));
    }

    private User saveUser(String name, UserRole role) {
        User user = new User();
        user.setName(name);
        user.setEmail("details-" + System.nanoTime() + "@test.dev");
        user.setPassword("secret");
        user.setPhone("+9100" + (System.nanoTime() % 1_000_000_000L));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }
}
