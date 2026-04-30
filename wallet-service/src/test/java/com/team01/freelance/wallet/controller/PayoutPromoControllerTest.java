package com.team01.freelance.wallet.controller;

import com.team01.freelance.wallet.model.PayoutPromo;
import com.team01.freelance.wallet.service.PayoutPromoService;
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

class PayoutPromoControllerTest {

    private MockMvc mockMvc;
    private PayoutPromoService payoutPromoService;

    @BeforeEach
    void setUp() {
        PayoutPromoController controller = new PayoutPromoController();
        payoutPromoService = mock(PayoutPromoService.class);
        ReflectionTestUtils.setField(controller, "payoutPromoService", payoutPromoService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getAllReturnsOk() throws Exception {
        when(payoutPromoService.getAllPayoutPromos()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/payout-promos"))
                .andExpect(status().isOk());
    }

    @Test
    void getByIdReturnsOk() throws Exception {
        PayoutPromo payoutPromo = new PayoutPromo();
        when(payoutPromoService.getPayoutPromoById(1L)).thenReturn(Optional.of(payoutPromo));

        mockMvc.perform(get("/api/payout-promos/{id}", 1L))
                .andExpect(status().isOk());
    }

    @Test
    void createReturnsOk() throws Exception {
        PayoutPromo payoutPromo = new PayoutPromo();
        when(payoutPromoService.createPayoutPromo(any(PayoutPromo.class))).thenReturn(payoutPromo);

        mockMvc.perform(post("/api/payout-promos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateReturnsOk() throws Exception {
        PayoutPromo payoutPromo = new PayoutPromo();
        when(payoutPromoService.updatePayoutPromo(eq(1L), any(PayoutPromo.class))).thenReturn(payoutPromo);

        mockMvc.perform(put("/api/payout-promos/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteByIdReturnsNoContent() throws Exception {
        when(payoutPromoService.deletePayoutPromoById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/payout-promos/{id}", 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteAllReturnsNoContent() throws Exception {
        doNothing().when(payoutPromoService).deleteAllPayoutPromos();

        mockMvc.perform(delete("/api/payout-promos/all"))
                .andExpect(status().isNoContent());
    }
}
