package com.team01.freelance.wallet.service;

import com.team01.freelance.wallet.model.PayoutPromo;
import com.team01.freelance.wallet.repository.PayoutPromoRepository;
import com.team01.freelance.wallet.repository.PayoutRepository;
import com.team01.freelance.wallet.repository.PromoCodeRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
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

    public Optional<PayoutPromo> getPayoutPromoById(Long id) {
        return payoutPromoRepository.findById(id);
    }

    public PayoutPromo createPayoutPromo(PayoutPromo payoutPromo) {
        if (payoutPromo.getPayout() != null && payoutPromo.getPayout().getId() != null) {
            payoutPromo.setPayout(payoutRepository.findById(payoutPromo.getPayout().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Payout not found with id: " + payoutPromo.getPayout().getId())));
        }
        if (payoutPromo.getPromoCode() != null && payoutPromo.getPromoCode().getId() != null) {
            payoutPromo.setPromoCode(promoCodeRepository.findById(payoutPromo.getPromoCode().getId())
                    .orElseThrow(() -> new EntityNotFoundException("PromoCode not found with id: " + payoutPromo.getPromoCode().getId())));
        }
        return payoutPromoRepository.save(payoutPromo);
    }

    /**
     * Updates an existing payout promo and throws if it does not exist.
     * Validates the existence of the associated payout and promo code if provided.
     *
     * @param id The ID of the payout promo to update
     * @param payoutPromo The object containing updated fields
     * @return The updated payout promo
     * @throws EntityNotFoundException if the payout promo, payout, or promo code is not found
     */
    public PayoutPromo updatePayoutPromo(Long id, PayoutPromo payoutPromo) {
        return payoutPromoRepository.findById(id).map(existing -> {
            if (payoutPromo.getDiscountApplied() != null) {
                existing.setDiscountApplied(payoutPromo.getDiscountApplied());
            }
            if (payoutPromo.getAppliedAt() != null) {
                existing.setAppliedAt(payoutPromo.getAppliedAt());
            }
            if (payoutPromo.getPayout() != null && payoutPromo.getPayout().getId() != null) {
                existing.setPayout(payoutRepository.findById(payoutPromo.getPayout().getId())
                        .orElseThrow(() -> new EntityNotFoundException("Payout not found with id: " + payoutPromo.getPayout().getId())));
            }
            if (payoutPromo.getPromoCode() != null && payoutPromo.getPromoCode().getId() != null) {
                existing.setPromoCode(promoCodeRepository.findById(payoutPromo.getPromoCode().getId())
                        .orElseThrow(() -> new EntityNotFoundException("PromoCode not found with id: " + payoutPromo.getPromoCode().getId())));
            }
            return payoutPromoRepository.save(existing);
        }).orElseThrow(() -> new EntityNotFoundException("Payout Promo not found with id: " + id));
    }

    public boolean deletePayoutPromoById(Long id) {
        if (!payoutPromoRepository.existsById(id)) {
            return false;
        }
        payoutPromoRepository.deleteById(id);
        return true;
    }

    public void deleteAllPayoutPromos() {
        payoutPromoRepository.deleteAll();
    }
}
