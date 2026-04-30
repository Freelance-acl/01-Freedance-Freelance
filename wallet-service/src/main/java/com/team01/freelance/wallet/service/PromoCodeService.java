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
        return promoCodeRepository.findById(id).map(existing -> {
            if (promoCode.getCode() != null) {
                existing.setCode(promoCode.getCode());
            }
            if (promoCode.getDiscountType() != null) {
                existing.setDiscountType(promoCode.getDiscountType());
            }
            if (promoCode.getDiscountValue() != null) {
                existing.setDiscountValue(promoCode.getDiscountValue());
            }
            if (promoCode.getMaxUses() != null) {
                existing.setMaxUses(promoCode.getMaxUses());
            }
            if (promoCode.getCurrentUses() != null) {
                existing.setCurrentUses(promoCode.getCurrentUses());
            }
            if (promoCode.getExpiryDate() != null) {
                existing.setExpiryDate(promoCode.getExpiryDate());
            }
            if (promoCode.getActive() != null) {
                existing.setActive(promoCode.getActive());
            }
            if (promoCode.getMetadata() != null) {
                existing.setMetadata(promoCode.getMetadata());
            }
            return promoCodeRepository.save(existing);
        });
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
