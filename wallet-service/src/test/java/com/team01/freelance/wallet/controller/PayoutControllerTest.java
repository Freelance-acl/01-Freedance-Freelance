package com.team01.freelance.wallet.controller;

import com.team01.freelance.wallet.dto.FreelancerPayoutSummaryDTO;
import com.team01.freelance.wallet.dto.PayoutDetailsDTO;
import com.team01.freelance.wallet.dto.ProcessPayoutRequest;
import com.team01.freelance.wallet.dto.PromoCodeUsageDTO;
import com.team01.freelance.wallet.dto.RevenueReportDTO;
import com.team01.freelance.wallet.exception.GlobalExceptionHandler;
import com.team01.freelance.wallet.model.Payout;
import com.team01.freelance.wallet.model.PayoutMethod;
import com.team01.freelance.wallet.model.PayoutStatus;
import com.team01.freelance.wallet.service.PayoutService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
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
    void getByIdReturnsNotFound() throws Exception {
        when(payoutService.getPayoutById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/payouts/{id}", 999L))
                .andExpect(status().isNotFound());
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
    void updateReturnsNotFound() throws Exception {
        when(payoutService.updatePayout(eq(999L), any(Payout.class)))
                .thenThrow(new EntityNotFoundException("Payout not found with id: 999"));

        mockMvc.perform(put("/api/payouts/{id}", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteByIdReturnsNoContent() throws Exception {
        when(payoutService.deletePayoutById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/payouts/{id}", 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteByIdReturnsNotFound() throws Exception {
        when(payoutService.deletePayoutById(999L)).thenReturn(false);

        mockMvc.perform(delete("/api/payouts/{id}", 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteAllReturnsNoContent() throws Exception {
        doNothing().when(payoutService).deleteAllPayouts();

        mockMvc.perform(delete("/api/payouts/all"))
                .andExpect(status().isNoContent());
    }

    @Test
    void searchReturnsOk() throws Exception {
        when(payoutService.searchPayouts(PayoutStatus.COMPLETED, LocalDate.parse("2026-03-01"), LocalDate.parse("2026-03-31")))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/payouts/search")
                        .param("status", "COMPLETED")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31"))
                .andExpect(status().isOk());
    }


    // -----------------------------------------------------------------------
    // [S5-F2] Refund Payout
    // -----------------------------------------------------------------------

    @Test
    void refundPayout_returnsOkWithRefundedStatus() throws Exception {
        Payout payout = new Payout();
        payout.setId(1L);
        payout.setStatus(PayoutStatus.REFUNDED);
        when(payoutService.refundPayout(1L, "duplicate charge")).thenReturn(payout);

        mockMvc.perform(put("/api/payouts/{id}/refund", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"duplicate charge\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));
    }

    @Test
    void refundPayout_notFound_returns404() throws Exception {
        when(payoutService.refundPayout(999L, "reason"))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Payout not found with id: 999"));

        mockMvc.perform(put("/api/payouts/{id}/refund", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"reason\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Payout not found")));
    }

    @Test
    void refundPayout_notCompleted_returns400() throws Exception {
        when(payoutService.refundPayout(1L, "reason"))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Only COMPLETED payouts can be refunded"));

        mockMvc.perform(put("/api/payouts/{id}/refund", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"reason\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("COMPLETED")));
    }

    // -----------------------------------------------------------------------
    // Payout details & top promos
    // -----------------------------------------------------------------------

    @Test
    void getDetails_returnsOkWithPayoutFields() throws Exception {
        PayoutDetailsDTO dto = PayoutDetailsDTO.builder()
                .payoutId(1L)
                .contractId(42L)
                .freelancerId(100L)
                .originalAmount(1500.0)
                .method(com.team01.freelance.wallet.model.PayoutMethod.BANK_TRANSFER)
                .status(PayoutStatus.COMPLETED)
                .transactionDetails(new java.util.HashMap<>())
                .appliedPromoCodes(new java.util.ArrayList<>())
                .totalDiscount(0.0)
                .finalAmount(1500.0)
                .build();

        when(payoutService.getPayoutDetails(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/payouts/{payoutId}/details", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payoutId").value(1))
                .andExpect(jsonPath("$.contractId").value(42))
                .andExpect(jsonPath("$.originalAmount").value(1500.0))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void getDetails_notFound_returns404() throws Exception {
        when(payoutService.getPayoutDetails(999L))
                .thenThrow(new EntityNotFoundException("Payout not found with id: 999"));

        mockMvc.perform(get("/api/payouts/{payoutId}/details", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Payout not found")));
    }

    @Test
    void getTopUsedPromos_returnsOkWithUsageList() throws Exception {
        // Only chain the fields strictly required by the test scenario assertions
        PromoCodeUsageDTO usage = PromoCodeUsageDTO.builder()
                .code("SAVE10")
                .timesUsed(5L)
                .build();

        when(payoutService.getTopUsedPromoCodes(3)).thenReturn(List.of(usage));

        mockMvc.perform(get("/api/payouts/promos/top-used").param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].code").value("SAVE10"))
                .andExpect(jsonPath("$[0].timesUsed").value(5));
    }

    // -----------------------------------------------------------------------
    // Retry failed payout
    // -----------------------------------------------------------------------

    @Test
    void retryPayout_returnsOkWithCompletedStatus() throws Exception {
        Payout payout = new Payout();
        payout.setId(1L);
        payout.setStatus(PayoutStatus.COMPLETED);
        when(payoutService.retryPayout(1L)).thenReturn(payout);

        mockMvc.perform(put("/api/payouts/{id}/retry", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void retryPayout_notFound_returns404() throws Exception {
        when(payoutService.retryPayout(999L))
                .thenThrow(new EntityNotFoundException("Payout not found with id: 999"));

        mockMvc.perform(put("/api/payouts/{id}/retry", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Payout not found")));
    }

    @Test
    void retryPayout_notFailed_returns400() throws Exception {
        when(payoutService.retryPayout(1L))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Only FAILED payouts can be retried"));

        mockMvc.perform(put("/api/payouts/{id}/retry", 1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("FAILED")));
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

    // -----------------------------------------------------------------------
    // [S5-F4] Process Payout for Contract (M3 Refactor)
    // -----------------------------------------------------------------------

    @Test
    void processContractPayout_success_returns2xx() throws Exception {
        Long contractId = 42L;
        Long callerId = 42L;
        String callerRole = "CLIENT";

        Payout payout = new Payout();
        payout.setId(10L);
        payout.setContractId(contractId);
        payout.setStatus(PayoutStatus.COMPLETED);
        payout.setMethod(PayoutMethod.BANK_TRANSFER);

        when(payoutService.processContractPayout(eq(contractId), any(ProcessPayoutRequest.class), eq(callerId), eq(callerRole)))
                .thenReturn(payout);

        mockMvc.perform(post("/api/payouts/contract/{contractId}", contractId)
                        .header("X-User-Id", callerId)
                        .header("X-User-Role", callerRole)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"method":"BANK_TRANSFER","accountLastFour":"9876"}
                                """))
                .andExpect(status().is2xxSuccessful()) // Handles either 201 or 200 OK from the idempotency block
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.method").value("BANK_TRANSFER"));
    }

    @Test
    void processContractPayout_alreadyProcessed_returns200Idempotent() throws Exception {
        Long contractId = 42L;
        Long callerId = 42L;
        String callerRole = "CLIENT";

        Payout existingPayout = new Payout();
        existingPayout.setId(10L);
        existingPayout.setContractId(contractId);
        existingPayout.setStatus(PayoutStatus.COMPLETED);
        existingPayout.setMethod(PayoutMethod.BANK_TRANSFER);

        when(payoutService.processContractPayout(eq(contractId), any(ProcessPayoutRequest.class), eq(callerId), eq(callerRole)))
                .thenReturn(existingPayout);

        mockMvc.perform(post("/api/payouts/contract/{contractId}", contractId)
                        .header("X-User-Id", callerId)
                        .header("X-User-Role", callerRole)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"BANK_TRANSFER\"}"))
                .andExpect(status().isOk()) // Idempotency check explicitly returns 200 OK
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void processContractPayout_contractNotCompleted_returns400() throws Exception {
        Long contractId = 7L;
        Long callerId = 42L;
        String callerRole = "CLIENT";

        when(payoutService.processContractPayout(eq(contractId), any(ProcessPayoutRequest.class), eq(callerId), eq(callerRole)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contract is not completed. Status: ACTIVE"));

        mockMvc.perform(post("/api/payouts/contract/{contractId}", contractId)
                        .header("X-User-Id", callerId)
                        .header("X-User-Role", callerRole)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"PAYPAL\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("not completed")));
    }

    @Test
    void processContractPayout_forbidden_returns403() throws Exception {
        Long contractId = 42L;
        Long callerId = 99L; // Different user trying to release the payout
        String callerRole = "CLIENT";

        when(payoutService.processContractPayout(eq(contractId), any(ProcessPayoutRequest.class), eq(callerId), eq(callerRole)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the contract's client (or an ADMIN) can release this payout"));

        mockMvc.perform(post("/api/payouts/contract/{contractId}", contractId)
                        .header("X-User-Id", callerId)
                        .header("X-User-Role", callerRole)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"BANK_TRANSFER\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("ADMIN")));
    }

    @Test
    void processContractPayout_contractNotFound_returns404() throws Exception {
        Long contractId = 999L;
        Long callerId = 42L;
        String callerRole = "CLIENT";

        when(payoutService.processContractPayout(eq(contractId), any(ProcessPayoutRequest.class), eq(callerId), eq(callerRole)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Contract not found after retries"));

        mockMvc.perform(post("/api/payouts/contract/{contractId}", contractId)
                        .header("X-User-Id", callerId)
                        .header("X-User-Role", callerRole)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"BANK_TRANSFER\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Contract not found")));
    }

    // -----------------------------------------------------------------------
    // [S5-F5] Apply Promo Code to Payout
    // -----------------------------------------------------------------------

    @Test
    void applyPromoCode_returns201() throws Exception {
        Long payoutId = 1L;
        Long promoCodeId = 2L;
        Payout payout = new Payout();
        payout.setId(payoutId);
        payout.setStatus(PayoutStatus.PENDING);
        payout.setAmount(3000.0);

        when(payoutService.applyPromoCode(payoutId, promoCodeId)).thenReturn(payout);

        mockMvc.perform(post("/api/payouts/{payoutId}/promos/{promoCodeId}", payoutId, promoCodeId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void applyPromoCode_notFound_returns404() throws Exception {
        Long payoutId = 1L;
        Long promoCodeId = 2L;
        when(payoutService.applyPromoCode(payoutId, promoCodeId))
                .thenThrow(new EntityNotFoundException("Payout not found with id: " + payoutId));

        mockMvc.perform(post("/api/payouts/{payoutId}/promos/{promoCodeId}", payoutId, promoCodeId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Payout not found")));
    }

    @Test
    void applyPromoCode_alreadyApplied_returns400() throws Exception {
        Long payoutId = 1L;
        Long promoCodeId = 2L;
        when(payoutService.applyPromoCode(payoutId, promoCodeId))
                .thenThrow(new IllegalStateException("Promo code already applied to this payout"));

        mockMvc.perform(post("/api/payouts/{payoutId}/promos/{promoCodeId}", payoutId, promoCodeId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already applied")));
    }

    // -----------------------------------------------------------------------
    // [S5-F6] Revenue Report
    // -----------------------------------------------------------------------

    @Test
    void getRevenueReport_returnsOkWithMetrics() throws Exception {
        RevenueReportDTO dto = new RevenueReportDTO(10000.0, 5L, 2000.0, 2000.0, 2L);
        when(payoutService.getRevenueReport(
                LocalDate.parse("2026-03-01"), LocalDate.parse("2026-03-31")))
                .thenReturn(dto);

        mockMvc.perform(get("/api/payouts/reports/revenue")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRevenue").value(10000.0))
                .andExpect(jsonPath("$.totalTransactions").value(5))
                .andExpect(jsonPath("$.averagePayout").value(2000.0))
                .andExpect(jsonPath("$.refundedAmount").value(2000.0))
                .andExpect(jsonPath("$.refundCount").value(2));
    }

    @Test
    void getRevenueReport_invalidDateRange_returns400() throws Exception {
        when(payoutService.getRevenueReport(
                LocalDate.parse("2026-03-31"), LocalDate.parse("2026-03-01")))
                .thenThrow(new IllegalStateException("startDate cannot be after endDate"));

        mockMvc.perform(get("/api/payouts/reports/revenue")
                        .param("startDate", "2026-03-31")
                        .param("endDate", "2026-03-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("startDate")));
    }
}
