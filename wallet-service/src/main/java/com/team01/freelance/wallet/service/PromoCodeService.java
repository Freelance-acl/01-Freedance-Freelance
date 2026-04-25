package com.team01.freelance.wallet.service;

import com.team01.freelance.wallet.model.PromoCode;
import com.team01.freelance.wallet.repository.PromoCodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PromoCodeService {

    @Autowired
    private PromoCodeRepository promoCodeRepository;

    public List<PromoCode> getAllPromoCodes() {
        return promoCodeRepository.findAll();
    }

    public Optional<PromoCode> getPromoCodeById(Long id) {
        return promoCodeRepository.findById(id);
    }

    public PromoCode createPromoCode(PromoCode promoCode) {
        return promoCodeRepository.save(promoCode);
    }

    public Optional<PromoCode> updatePromoCode(Long id, PromoCode promoCode) {
        if (!promoCodeRepository.existsById(id)) {
            return Optional.empty();
        }
        promoCode.setId(id);
        return Optional.of(promoCodeRepository.save(promoCode));
    }

    public boolean deletePromoCodeById(Long id) {
        if (!promoCodeRepository.existsById(id)) {
            return false;
        }
        promoCodeRepository.deleteById(id);
        return true;
    }

    public void deleteAllPromoCodes() {
        promoCodeRepository.deleteAll();
    }
}
