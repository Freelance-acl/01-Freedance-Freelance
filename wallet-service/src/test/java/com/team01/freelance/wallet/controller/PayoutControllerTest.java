package com.team01.freelance.wallet.controller;

import com.team01.freelance.wallet.dto.FreelancerPayoutSummaryDTO;
import com.team01.freelance.wallet.exception.GlobalExceptionHandler;
import com.team01.freelance.wallet.model.Payout;
import com.team01.freelance.wallet.service.PayoutService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PayoutControllerTest {

    private MockMvc mockMvc;
    private PayoutService payoutService;

    @BeforeEach
    void setUp() {
        PayoutController controller = new PayoutController();
        payoutService = mock(PayoutService.class);
        ReflectionTestUtils.setField(controller, "payoutService", payoutService);
        // Register the global advice so ResponseStatusException maps to the
        // proper HTTP status (instead of being swallowed and returned as 500).
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getAllReturnsOk() throws Exception {
        when(payoutService.getAllPayouts()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/payouts"))
                .andExpect(status().isOk());
    }

    @Test
    void getByIdReturnsOk() throws Exception {
        Payout payout = new Payout();
        when(payoutService.getPayoutById(1L)).thenReturn(Optional.of(payout));

        mockMvc.perform(get("/api/payouts/{id}", 1L))
                .andExpect(status().isOk());
    }

    @Test
    void createReturnsOk() throws Exception {
        Payout payout = new Payout();
        when(payoutService.createPayout(any(Payout.class))).thenReturn(payout);

        mockMvc.perform(post("/api/payouts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateReturnsOk() throws Exception {
        Payout payout = new Payout();
        when(payoutService.updatePayout(eq(1L), any(Payout.class))).thenReturn(payout);

        mockMvc.perform(put("/api/payouts/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteByIdReturnsNoContent() throws Exception {
        when(payoutService.deletePayoutById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/payouts/{id}", 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteAllReturnsNoContent() throws Exception {
        doNothing().when(payoutService).deleteAllPayouts();

        mockMvc.perform(delete("/api/payouts/all"))
                .andExpect(status().isNoContent());
    }

    // -----------------------------------------------------------------------
    // [S5-F3] Freelancer Payout Summary
    // -----------------------------------------------------------------------

    @Test
    void getFreelancerSummary_returnsOkWithBreakdown() throws Exception {
        Long freelancerId = 1L;
        Map<String, Double> breakdown = new LinkedHashMap<>();
        breakdown.put("BANK_TRANSFER", 3500.0);
        breakdown.put("PAYPAL", 800.0);
        breakdown.put("CRYPTO", 500.0);

        FreelancerPayoutSummaryDTO dto = new FreelancerPayoutSummaryDTO(
                freelancerId, 4L, 4800.0, breakdown);

        when(payoutService.getFreelancerPayoutSummary(freelancerId)).thenReturn(dto);

        mockMvc.perform(get("/api/payouts/freelancer/{freelancerId}/summary", freelancerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.freelancerId").value(1))
                .andExpect(jsonPath("$.totalPayouts").value(4))
                .andExpect(jsonPath("$.totalAmount").value(4800.0))
                .andExpect(jsonPath("$.methodBreakdown.BANK_TRANSFER").value(3500.0))
                .andExpect(jsonPath("$.methodBreakdown.PAYPAL").value(800.0))
                .andExpect(jsonPath("$.methodBreakdown.CRYPTO").value(500.0));
    }

    @Test
    void getFreelancerSummary_unknownUserReturns404() throws Exception {
        Long freelancerId = 9999L;
        when(payoutService.getFreelancerPayoutSummary(freelancerId))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found with id: " + freelancerId));

        mockMvc.perform(get("/api/payouts/freelancer/{freelancerId}/summary", freelancerId))
                .andExpect(status().isNotFound());
    }
}
