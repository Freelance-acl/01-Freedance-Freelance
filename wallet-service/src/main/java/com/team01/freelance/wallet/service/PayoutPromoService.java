package com.team01.freelance.wallet.service;

import com.team01.freelance.wallet.model.PayoutPromo;
import com.team01.freelance.wallet.repository.PayoutPromoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PayoutPromoService {

    @Autowired
    private PayoutPromoRepository payoutPromoRepository;

    public List<PayoutPromo> getAllPayoutPromos() {
        return payoutPromoRepository.findAll();
    }

    public Optional<PayoutPromo> getPayoutPromoById(Long id) {
        return payoutPromoRepository.findById(id);
    }

    public PayoutPromo createPayoutPromo(PayoutPromo payoutPromo) {
        return payoutPromoRepository.save(payoutPromo);
    }

    public Optional<PayoutPromo> updatePayoutPromo(Long id, PayoutPromo payoutPromo) {
        return payoutPromoRepository.findById(id).map(existing -> {
            if (payoutPromo.getDiscountApplied() != null) {
                existing.setDiscountApplied(payoutPromo.getDiscountApplied());
            }
            if (payoutPromo.getAppliedAt() != null) {
                existing.setAppliedAt(payoutPromo.getAppliedAt());
            }
            if (payoutPromo.getPayout() != null) {
                existing.setPayout(payoutPromo.getPayout());
            }
            if (payoutPromo.getPromoCode() != null) {
                existing.setPromoCode(payoutPromo.getPromoCode());
            }
            return payoutPromoRepository.save(existing);
        });
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
