package com.team01.freelance.wallet.controller;

import com.team01.freelance.wallet.dto.PayoutMethodDTO;
import com.team01.freelance.wallet.model.PayoutMethod;
import com.team01.freelance.wallet.service.PayoutService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class PayoutAnalyticsIntegrationTest {

    private MockMvc mockMvc;
    private PayoutService payoutService;

    @BeforeEach
    void setup() {
        payoutService = Mockito.mock(PayoutService.class);

        PayoutController controller = new PayoutController();
        ReflectionTestUtils.setField(controller, "payoutService", payoutService);

        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void testS5F11_MethodBreakdown_Success() throws Exception {
        when(payoutService.getPayoutMethodBreakdown(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(
                        new PayoutMethodDTO(PayoutMethod.BANK_TRANSFER, 2, 1, 0.66, 2000.0),
                        new PayoutMethodDTO(PayoutMethod.PAYPAL, 1, 0, 1.0, 500.0)
                ));

        mockMvc.perform(get("/api/payouts/analytics/methods")
                        .param("startDate", "2026-06-01")
                        .param("endDate", "2026-06-02")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].method").value("BANK_TRANSFER"))
                .andExpect(jsonPath("$[0].successCount").value(2))
                .andExpect(jsonPath("$[1].method").value("PAYPAL"))
                .andExpect(jsonPath("$[1].totalAmount").value(500.0));
    }
}