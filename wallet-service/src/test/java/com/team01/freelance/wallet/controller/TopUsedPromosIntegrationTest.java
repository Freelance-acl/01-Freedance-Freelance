package com.team01.freelance.wallet.controller;

import com.team01.freelance.wallet.model.DiscountType;
import com.team01.freelance.wallet.model.PromoCode;
import com.team01.freelance.wallet.repository.PromoCodeRepository;
import com.team01.freelance.wallet.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.test.context.support.WithMockUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [S5-F9] Integration tests for {@code GET /api/payouts/promos/top-used}.
 */
@Transactional
@WithMockUser(roles = "ADMIN")
class TopUsedPromosIntegrationTest extends AbstractIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private PromoCodeRepository promoCodeRepository;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(webApplicationContext);
    }

    @Test
    void topUsedPromos_returnsPromosOrderedByUsage() throws Exception {
        savePromo("PROMO_A", 5);
        savePromo("PROMO_B", 10);
        savePromo("PROMO_C", 2);

        mockMvc.perform(get("/api/payouts/promos/top-used").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].code").value("PROMO_B"))
                .andExpect(jsonPath("$[0].timesUsed").value(10))
                .andExpect(jsonPath("$[1].code").value("PROMO_A"));
    }

    private void savePromo(String code, int currentUses) {
        PromoCode promo = new PromoCode();
        promo.setCode(code);
        promo.setDiscountType(DiscountType.FIXED);
        promo.setDiscountValue(50.0);
        promo.setMaxUses(100);
        promo.setCurrentUses(currentUses);
        promo.setActive(true);
        promo.setExpiryDate(LocalDateTime.now().plusDays(30));
        promoCodeRepository.save(promo);
    }
}