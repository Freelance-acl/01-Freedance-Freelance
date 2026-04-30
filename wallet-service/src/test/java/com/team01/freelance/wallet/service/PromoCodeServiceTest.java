package com.team01.freelance.wallet.service;

import com.team01.freelance.wallet.model.DiscountType;
import com.team01.freelance.wallet.model.PromoCode;
import com.team01.freelance.wallet.repository.PromoCodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PromoCodeServiceTest {

    @Mock
    private PromoCodeRepository promoCodeRepository;

    @InjectMocks
    private PromoCodeService promoCodeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void updatePromoCode_ShouldMergeNonNullFields() {
        // Arrange
        Long id = 1L;
        PromoCode existing = new PromoCode();
        existing.setId(id);
        existing.setCode("OLDCODE");
        existing.setDiscountType(DiscountType.PERCENTAGE);
        existing.setDiscountValue(10.0);
        existing.setActive(true);

        PromoCode incoming = new PromoCode();
        incoming.setCode("NEWCODE");
        incoming.setDiscountValue(20.0);
        // discountType and active are null in incoming

        when(promoCodeRepository.findById(id)).thenReturn(Optional.of(existing));
        when(promoCodeRepository.save(any(PromoCode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        PromoCode result = promoCodeService.updatePromoCode(id, incoming);

        // Assert
        assertNotNull(result);
        PromoCode updated = result;
        assertEquals(id, updated.getId());
        assertEquals("NEWCODE", updated.getCode()); // Updated
        assertEquals(20.0, updated.getDiscountValue()); // Updated
        assertEquals(DiscountType.PERCENTAGE, updated.getDiscountType()); // Preserved
        assertTrue(updated.getActive()); // Preserved
        
        verify(promoCodeRepository).findById(id);
        verify(promoCodeRepository).save(updated);
    }
}
