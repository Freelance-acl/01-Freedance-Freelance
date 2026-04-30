package com.team01.freelance.wallet.controller;

import com.team01.freelance.wallet.model.PromoCode;
import com.team01.freelance.wallet.service.PromoCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PromoCodeControllerTest {

    private MockMvc mockMvc;
    private PromoCodeService promoCodeService;

    @BeforeEach
    void setUp() {
        PromoCodeController controller = new PromoCodeController();
        promoCodeService = mock(PromoCodeService.class);
        ReflectionTestUtils.setField(controller, "promoCodeService", promoCodeService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getAllReturnsOk() throws Exception {
        when(promoCodeService.getAllPromoCodes()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/promo-codes"))
                .andExpect(status().isOk());
    }

    @Test
    void getByIdReturnsOk() throws Exception {
        PromoCode promoCode = new PromoCode();
        when(promoCodeService.getPromoCodeById(1L)).thenReturn(Optional.of(promoCode));

        mockMvc.perform(get("/api/promo-codes/{id}", 1L))
                .andExpect(status().isOk());
    }

    @Test
    void createReturnsOk() throws Exception {
        PromoCode promoCode = new PromoCode();
        when(promoCodeService.createPromoCode(any(PromoCode.class))).thenReturn(promoCode);

        mockMvc.perform(post("/api/promo-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateReturnsOk() throws Exception {
        PromoCode promoCode = new PromoCode();
        when(promoCodeService.updatePromoCode(eq(1L), any(PromoCode.class))).thenReturn(promoCode);

        mockMvc.perform(put("/api/promo-codes/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteByIdReturnsNoContent() throws Exception {
        when(promoCodeService.deletePromoCodeById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/promo-codes/{id}", 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteAllReturnsNoContent() throws Exception {
        doNothing().when(promoCodeService).deleteAllPromoCodes();

        mockMvc.perform(delete("/api/promo-codes/all"))
                .andExpect(status().isNoContent());
    }
}
