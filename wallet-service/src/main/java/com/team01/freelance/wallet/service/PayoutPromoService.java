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
        if (!payoutPromoRepository.existsById(id)) {
            return Optional.empty();
        }
        payoutPromo.setId(id);
        return Optional.of(payoutPromoRepository.save(payoutPromo));
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
