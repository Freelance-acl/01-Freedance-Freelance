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
import com.team01.freelance.wallet.model.PayoutPromo;
import com.team01.freelance.wallet.model.PayoutStatus;
import com.team01.freelance.wallet.model.PromoCode;
import com.team01.freelance.wallet.repository.PayoutPromoRepository;
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
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [S5-F5] Integration tests for {@code POST /api/payouts/{payoutId}/promos/{promoCodeId}}.
 */
@Transactional
class ApplyPromoCodeIntegrationTest extends AbstractIntegrationTest {

    private static final String APPLY_URL = "/api/payouts/{payoutId}/promos/{promoCodeId}";

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private PayoutRepository payoutRepository;

    @Autowired
    private PromoCodeRepository promoCodeRepository;

    @Autowired
    private PayoutPromoRepository payoutPromoRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private UserRepository userRepository;

    private User freelancer;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        User client = saveUser("Client", UserRole.CLIENT);
        freelancer = saveUser("Freelancer", UserRole.FREELANCER);
    }

    /** Spec (a)+(b): PERCENTAGE promo on 3000 EGP payout → discount 300, currentUses = 1. */
    @Test
    void applyPercentagePromo_createsPayoutPromoAndIncrementsUses() throws Exception {
        Payout payout = savePendingPayout(3000.0);
        PromoCode promo = savePromoCode("FIRSTJOB20", DiscountType.PERCENTAGE, 10.0, 5, true,
                LocalDateTime.now().plusDays(30));

        mockMvc.perform(post(APPLY_URL, payout.getId(), promo.getId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(payout.getId().intValue()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.amount").value(3000.0));

        List<PayoutPromo> links = payoutPromoRepository.findAll();
        assertEquals(1, links.size());
        assertEquals(300.0, links.get(0).getDiscountApplied());
        assertEquals(payout.getId(), links.get(0).getPayout().getId());
        assertEquals(promo.getId(), links.get(0).getPromoCode().getId());

        PromoCode updated = promoCodeRepository.findById(promo.getId()).orElseThrow();
        assertEquals(1, updated.getCurrentUses());
    }

    /** Spec (c): same promo applied twice → 400. */
    @Test
    void applySamePromoTwice_returns400AlreadyApplied() throws Exception {
        Payout payout = savePendingPayout(3000.0);
        PromoCode promo = savePromoCode("FIRSTJOB20", DiscountType.PERCENTAGE, 10.0, 5, true,
                LocalDateTime.now().plusDays(30));

        mockMvc.perform(post(APPLY_URL, payout.getId(), promo.getId()))
                .andExpect(status().isCreated());

        mockMvc.perform(post(APPLY_URL, payout.getId(), promo.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already applied")));
    }

    /** Spec (d): FIXED discount larger than payout amount → capped at payout amount. */
    @Test
    void applyFixedPromoExceedingAmount_capsDiscountAtPayoutAmount() throws Exception {
        Payout payout = savePendingPayout(3000.0);
        PromoCode promo = savePromoCode("BIGFIXED", DiscountType.FIXED, 9999.0, 5, true,
                LocalDateTime.now().plusDays(30));

        mockMvc.perform(post(APPLY_URL, payout.getId(), promo.getId()))
                .andExpect(status().isCreated());

        PayoutPromo link = payoutPromoRepository.findAll().get(0);
        assertEquals(3000.0, link.getDiscountApplied());
    }

    /** Spec (e): expired promo code → 400. */
    @Test
    void applyExpiredPromo_returns400() throws Exception {
        Payout payout = savePendingPayout(3000.0);
        PromoCode promo = savePromoCode("EXPIRED", DiscountType.PERCENTAGE, 10.0, 5, true,
                LocalDateTime.now().minusDays(1));

        mockMvc.perform(post(APPLY_URL, payout.getId(), promo.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("expired")));
    }

    /** Spec (e): inactive promo code → 400. */
    @Test
    void applyInactivePromo_returns400() throws Exception {
        Payout payout = savePendingPayout(3000.0);
        PromoCode promo = savePromoCode("INACTIVE", DiscountType.PERCENTAGE, 10.0, 5, false,
                LocalDateTime.now().plusDays(30));

        mockMvc.perform(post(APPLY_URL, payout.getId(), promo.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("not active")));
    }

    /** Spec (f): COMPLETED payout → 400. */
    @Test
    void applyPromoToCompletedPayout_returns400() throws Exception {
        Payout payout = savePendingPayout(3000.0);
        payout.setStatus(PayoutStatus.COMPLETED);
        payoutRepository.save(payout);

        PromoCode promo = savePromoCode("FIRSTJOB20", DiscountType.PERCENTAGE, 10.0, 5, true,
                LocalDateTime.now().plusDays(30));

        mockMvc.perform(post(APPLY_URL, payout.getId(), promo.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("completed")));
    }

    @Test
    void applyPromo_unknownPayout_returns404() throws Exception {
        PromoCode promo = savePromoCode("FIRSTJOB20", DiscountType.PERCENTAGE, 10.0, 5, true,
                LocalDateTime.now().plusDays(30));

        mockMvc.perform(post(APPLY_URL, 999_999L, promo.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Payout not found")));
    }

    @Test
    void applyPromo_unknownPromoCode_returns404() throws Exception {
        Payout payout = savePendingPayout(3000.0);

        mockMvc.perform(post(APPLY_URL, payout.getId(), 999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Promo code not found")));
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

    private Payout savePendingPayout(double amount) {
        Contract contract = new Contract();
        contract.setJobId(1L);
        contract.setProposalId(1L);
        contract.setClientId(saveUser("TmpClient", UserRole.CLIENT).getId());
        contract.setFreelancerId(freelancer.getId());
        contract.setAgreedAmount(amount);
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setStartDate(LocalDateTime.now().minusDays(7));
        contract = contractRepository.save(contract);

        Payout payout = new Payout();
        payout.setContractId(contract.getId());
        payout.setFreelancerId(freelancer.getId());
        payout.setAmount(amount);
        payout.setMethod(PayoutMethod.BANK_TRANSFER);
        payout.setStatus(PayoutStatus.PENDING);
        return payoutRepository.save(payout);
    }

    private PromoCode savePromoCode(String code, DiscountType type, double value, int maxUses,
                                    boolean active, LocalDateTime expiry) {
        PromoCode promo = new PromoCode();
        promo.setCode(code + "-" + System.nanoTime());
        promo.setDiscountType(type);
        promo.setDiscountValue(value);
        promo.setMaxUses(maxUses);
        promo.setCurrentUses(0);
        promo.setActive(active);
        promo.setExpiryDate(expiry);
        return promoCodeRepository.save(promo);
    }
}
