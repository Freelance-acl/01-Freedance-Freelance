package com.team01.freelance.wallet.service;

import com.team01.freelance.wallet.model.PayoutPromo;
import com.team01.freelance.wallet.repository.PayoutPromoRepository;
import com.team01.freelance.wallet.repository.PayoutRepository;
import com.team01.freelance.wallet.repository.PromoCodeRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PayoutPromoService {

    @Autowired
    private PayoutPromoRepository payoutPromoRepository;

    @Autowired
    private PayoutRepository payoutRepository;

    @Autowired
    private PromoCodeRepository promoCodeRepository;

    public List<PayoutPromo> getAllPayoutPromos() {
        return payoutPromoRepository.findAll();
    }

    @Cacheable(value = "payout-promo", key = "#id", unless = "#result == null")
    public Optional<PayoutPromo> getPayoutPromoById(Long id) {
        return payoutPromoRepository.findById(id);
    }

    public PayoutPromo createPayoutPromo(PayoutPromo payoutPromo) {
        if (payoutPromo.getPayout() == null || payoutPromo.getPayout().getId() == null) {
            throw new IllegalArgumentException("Payout ID is required to create a PayoutPromo");
        }
        if (payoutPromo.getPromoCode() == null || payoutPromo.getPromoCode().getId() == null) {
            throw new IllegalArgumentException("PromoCode ID is required to create a PayoutPromo");
        }

        payoutPromo.setPayout(payoutRepository.findById(payoutPromo.getPayout().getId())
                .orElseThrow(() -> new EntityNotFoundException("Payout not found with id: " + payoutPromo.getPayout().getId())));
        
        payoutPromo.setPromoCode(promoCodeRepository.findById(payoutPromo.getPromoCode().getId())
                .orElseThrow(() -> new EntityNotFoundException("PromoCode not found with id: " + payoutPromo.getPromoCode().getId())));

        return payoutPromoRepository.save(payoutPromo);
    }

    /**
     * Updates editable fields on an existing payout promo.
     * Associated payout and promo code cannot be changed after creation.
     *
     * @param id The ID of the payout promo to update
     * @param payoutPromo The object containing updated fields
     * @return The updated payout promo
     * @throws EntityNotFoundException if the payout promo is not found
     */
    @CacheEvict(value = "payout-promo", allEntries = true)
    public PayoutPromo updatePayoutPromo(Long id, PayoutPromo payoutPromo) {
        return payoutPromoRepository.findById(id).map(existing -> {
            if (payoutPromo.getDiscountApplied() != null) {
                existing.setDiscountApplied(payoutPromo.getDiscountApplied());
            }
            if (payoutPromo.getAppliedAt() != null) {
                existing.setAppliedAt(payoutPromo.getAppliedAt());
            }
            return payoutPromoRepository.save(existing);
        }).orElseThrow(() -> new EntityNotFoundException("Payout Promo not found with id: " + id));
    }

    @CacheEvict(value = "payout-promo", allEntries = true)
    public boolean deletePayoutPromoById(Long id) {
        if (!payoutPromoRepository.existsById(id)) {
            return false;
        }
        payoutPromoRepository.deleteById(id);
        return true;
    }

    @CacheEvict(value = "payout-promo", allEntries = true)
    public void deleteAllPayoutPromos() {
        payoutPromoRepository.deleteAll();
    }
}
