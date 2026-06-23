package com.team01.freelance.wallet.service;

import com.team01.freelance.contract.repository.ContractRepository;
import com.team01.freelance.user.repository.UserRepository;
import com.team01.freelance.wallet.dto.FreelancerPayoutSummaryDTO;
import com.team01.freelance.wallet.feign.UserServiceClient;
import com.team01.freelance.wallet.repository.PayoutRepository;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PayoutService} focused on [S5-F3] Freelancer Payout Summary.
 */
class PayoutServiceTest {

    @Mock
    private PayoutRepository payoutRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private PayoutService payoutService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Spec scenario (b): 4 COMPLETED payouts — 2 BANK_TRANSFER (1500, 2000),
     * 1 PAYPAL (800), 1 CRYPTO (500) → totalPayouts=4, totalAmount=4800,
     * methodBreakdown={BANK_TRANSFER:3500, PAYPAL:800, CRYPTO:500}.
     */
    @Test
    void getFreelancerPayoutSummary_returnsBreakdownAndTotals() {
        Long freelancerId = 1L;
        when(payoutRepository.aggregateCompletedByMethodForFreelancer(freelancerId))
                .thenReturn(List.of(
                        new Object[]{"BANK_TRANSFER", 2L, 3500.0},
                        new Object[]{"CRYPTO",        1L,  500.0},
                        new Object[]{"PAYPAL",        1L,  800.0}
                ));

        FreelancerPayoutSummaryDTO dto = payoutService.getFreelancerPayoutSummary(freelancerId);

        assertNotNull(dto);
        assertEquals(freelancerId, dto.getFreelancerId());
        assertEquals(4L, dto.getTotalPayouts());
        assertEquals(4800.0, dto.getTotalAmount());

        Map<String, Double> breakdown = dto.getMethodBreakdown();
        assertEquals(3, breakdown.size());
        assertEquals(3500.0, breakdown.get("BANK_TRANSFER"));
        assertEquals(800.0,  breakdown.get("PAYPAL"));
        assertEquals(500.0,  breakdown.get("CRYPTO"));

        verify(userServiceClient).getUser(freelancerId);
        verify(payoutRepository).aggregateCompletedByMethodForFreelancer(freelancerId);
    }

    /** A freelancer with no COMPLETED payouts → all zeros / empty map. */
    @Test
    void getFreelancerPayoutSummary_noPayouts_returnsZeroes() {
        Long freelancerId = 5L;
        when(payoutRepository.aggregateCompletedByMethodForFreelancer(freelancerId))
                .thenReturn(List.of());

        FreelancerPayoutSummaryDTO dto = payoutService.getFreelancerPayoutSummary(freelancerId);

        assertEquals(freelancerId, dto.getFreelancerId());
        assertEquals(0L, dto.getTotalPayouts());
        assertEquals(0.0, dto.getTotalAmount());
        assertTrue(dto.getMethodBreakdown().isEmpty());
    }

    /** Spec scenario (c): non-existent user → 404. */
    @Test
    void getFreelancerPayoutSummary_userNotFound_throws404() {
        Long freelancerId = 9999L;
        when(userServiceClient.getUser(freelancerId)).thenThrow(mock(FeignException.NotFound.class));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> payoutService.getFreelancerPayoutSummary(freelancerId));

        assertEquals(404, ex.getStatusCode().value());
        verify(userServiceClient).getUser(freelancerId);
        verify(payoutRepository, never()).aggregateCompletedByMethodForFreelancer(freelancerId);
    }

    /**
     * Even when SUM(amount) returns BigDecimal (some JDBC drivers do this),
     * the service must coerce to double via Number#doubleValue().
     */
    @Test
    void getFreelancerPayoutSummary_handlesNumericTypeVariance() {
        Long freelancerId = 7L;
        when(payoutRepository.aggregateCompletedByMethodForFreelancer(freelancerId))
                .thenReturn(List.<Object[]>of(
                        new Object[]{"BANK_TRANSFER", java.math.BigInteger.valueOf(2),
                                java.math.BigDecimal.valueOf(3500.0)}
                ));

        FreelancerPayoutSummaryDTO dto = payoutService.getFreelancerPayoutSummary(freelancerId);

        assertEquals(2L, dto.getTotalPayouts());
        assertEquals(3500.0, dto.getTotalAmount());
        assertEquals(3500.0, dto.getMethodBreakdown().get("BANK_TRANSFER"));
    }
}
