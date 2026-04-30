package com.team01.freelance.wallet.service;

import com.team01.freelance.wallet.model.PayoutPromo;
import com.team01.freelance.wallet.repository.PayoutPromoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PayoutPromoServiceTest {

    @Mock
    private PayoutPromoRepository payoutPromoRepository;

    @InjectMocks
    private PayoutPromoService payoutPromoService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void updatePayoutPromo_ShouldMergeNonNullFields() {
        // Arrange
        Long id = 1L;
        PayoutPromo existing = new PayoutPromo();
        existing.setId(id);
        existing.setDiscountApplied(15.0);

        PayoutPromo incoming = new PayoutPromo();
        incoming.setDiscountApplied(25.0);

        when(payoutPromoRepository.findById(id)).thenReturn(Optional.of(existing));
        when(payoutPromoRepository.save(any(PayoutPromo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Optional<PayoutPromo> result = payoutPromoService.updatePayoutPromo(id, incoming);

        // Assert
        assertTrue(result.isPresent());
        PayoutPromo updated = result.get();
        assertEquals(id, updated.getId());
        assertEquals(25.0, updated.getDiscountApplied()); // Updated
        
        verify(payoutPromoRepository).findById(id);
        verify(payoutPromoRepository).save(updated);
    }
}
