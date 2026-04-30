package com.team01.freelance.wallet.service;

import com.team01.freelance.wallet.model.Payout;
import com.team01.freelance.wallet.model.PayoutPromo;
import com.team01.freelance.wallet.model.PromoCode;
import com.team01.freelance.wallet.repository.PayoutPromoRepository;
import com.team01.freelance.wallet.repository.PayoutRepository;
import com.team01.freelance.wallet.repository.PromoCodeRepository;
import jakarta.persistence.EntityNotFoundException;
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

    @Mock
    private PayoutRepository payoutRepository;

    @Mock
    private PromoCodeRepository promoCodeRepository;

    @InjectMocks
    private PayoutPromoService payoutPromoService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void updatePayoutPromo_ShouldMergeNonNullFieldsAndFetchRelationships() {
        // Arrange
        Long id = 1L;
        PayoutPromo existing = new PayoutPromo();
        existing.setId(id);
        existing.setDiscountApplied(15.0);

        PayoutPromo incoming = new PayoutPromo();
        incoming.setDiscountApplied(25.0);
        
        Payout payout = new Payout();
        payout.setId(10L);
        incoming.setPayout(payout);
        
        PromoCode promoCode = new PromoCode();
        promoCode.setId(20L);
        incoming.setPromoCode(promoCode);

        when(payoutPromoRepository.findById(id)).thenReturn(Optional.of(existing));
        when(payoutRepository.findById(10L)).thenReturn(Optional.of(payout));
        when(promoCodeRepository.findById(20L)).thenReturn(Optional.of(promoCode));
        when(payoutPromoRepository.save(any(PayoutPromo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        PayoutPromo result = payoutPromoService.updatePayoutPromo(id, incoming);

        // Assert
        assertNotNull(result);
        PayoutPromo updated = result;
        assertEquals(id, updated.getId());
        assertEquals(25.0, updated.getDiscountApplied());
        assertEquals(payout, updated.getPayout());
        assertEquals(promoCode, updated.getPromoCode());
        
        verify(payoutPromoRepository).findById(id);
        verify(payoutRepository).findById(10L);
        verify(promoCodeRepository).findById(20L);
        verify(payoutPromoRepository).save(updated);
    }

    @Test
    void updatePayoutPromo_ShouldThrowExceptionIfPayoutNotFound() {
        // Arrange
        Long id = 1L;
        PayoutPromo existing = new PayoutPromo();
        existing.setId(id);

        PayoutPromo incoming = new PayoutPromo();
        Payout payout = new Payout();
        payout.setId(10L);
        incoming.setPayout(payout);

        when(payoutPromoRepository.findById(id)).thenReturn(Optional.of(existing));
        when(payoutRepository.findById(10L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> payoutPromoService.updatePayoutPromo(id, incoming));
    }
}
