package com.team01.freelance.wallet.service;

import com.team01.freelance.wallet.dto.ProcessPayoutRequest;
import com.team01.freelance.wallet.model.DiscountType;
import com.team01.freelance.wallet.model.Payout;
import com.team01.freelance.wallet.model.PayoutPromo;
import com.team01.freelance.wallet.model.PayoutStatus;
import com.team01.freelance.wallet.model.PromoCode;
import com.team01.freelance.wallet.repository.PayoutPromoRepository;
import com.team01.freelance.wallet.repository.PayoutRepository;
import com.team01.freelance.wallet.repository.PromoCodeRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class PayoutService {

    @Autowired
    private PayoutRepository payoutRepository;

    @Autowired
    private PayoutPromoRepository payoutPromoRepository;

    @Autowired
    private PromoCodeRepository promoCodeRepository;

    public List<Payout> getAllPayouts() {
        return payoutRepository.findAll();
    }

    public Optional<Payout> getPayoutById(Long id) {
        return payoutRepository.findById(id);
    }

    public Payout createPayout(Payout payout) {
        if (payout.getContractId() == null || payout.getFreelancerId() == null) {
            throw new IllegalArgumentException("Contract and Freelancer IDs are required to create a Payout");
        }

        if (payoutRepository.countContractById(payout.getContractId()) == 0) {
            throw new EntityNotFoundException("Contract not found with id: " + payout.getContractId());
        }

        if (payoutRepository.countUserById(payout.getFreelancerId()) == 0) {
            throw new EntityNotFoundException("Freelancer not found with id: " + payout.getFreelancerId());
        }

        return payoutRepository.save(payout);
    }

    /**
     * Updates editable fields on an existing payout.
     * Link fields (contractId, freelancerId) are immutable after creation.
     */
    public Payout updatePayout(Long id, Payout payoutDetails) {
        return payoutRepository.findById(id).map(existingPayout -> {
                if (payoutDetails.getAmount() != null) existingPayout.setAmount(payoutDetails.getAmount());
                if (payoutDetails.getMethod() != null) existingPayout.setMethod(payoutDetails.getMethod());
                if (payoutDetails.getStatus() != null) existingPayout.setStatus(payoutDetails.getStatus());
                if (payoutDetails.getTransactionDetails() != null) existingPayout.setTransactionDetails(payoutDetails.getTransactionDetails());
                if (payoutDetails.getCreatedAt() != null) existingPayout.setCreatedAt(payoutDetails.getCreatedAt());
            return payoutRepository.save(existingPayout);
        }).orElseThrow(() -> new EntityNotFoundException("Payout not found with id: " + id));
    }

    public boolean deletePayoutById(Long id) {
        if (!payoutRepository.existsById(id)) {
            return false;
        }
        payoutRepository.deleteById(id);
        return true;
    }

    public void deleteAllPayouts() {
        payoutRepository.deleteAll();
    }

    // S5-F4
    public Payout processContractPayout(Long contractId, ProcessPayoutRequest request) {
        if (payoutRepository.countContractById(contractId) == 0) {
            throw new EntityNotFoundException("Contract not found with id: " + contractId);
        }

        String contractStatus = payoutRepository.findContractStatusById(contractId);
        if (!"COMPLETED".equalsIgnoreCase(contractStatus)) {
            throw new IllegalStateException("Contract " + contractId + " is not COMPLETED");
        }

        if (payoutRepository.existsByContractIdAndStatus(contractId, PayoutStatus.COMPLETED)) {
            throw new IllegalStateException("Payout already paid for contract: " + contractId);
        }

        Payout payout = payoutRepository.findByContractIdAndStatus(contractId, PayoutStatus.PENDING)
                .orElseThrow(() -> new EntityNotFoundException("No pending payout found for contract: " + contractId));

        payout.setStatus(PayoutStatus.COMPLETED);
        payout.setMethod(request.getMethod());

        Map<String, Object> txDetails = new LinkedHashMap<>();
        txDetails.put("transactionId", UUID.randomUUID().toString());
        txDetails.put("method", request.getMethod().name());
        if (request.getAccountLastFour() != null) {
            txDetails.put("accountLastFour", request.getAccountLastFour());
        }
        txDetails.put("processedAt", LocalDateTime.now().toString());
        payout.setTransactionDetails(txDetails);

        return payoutRepository.save(payout);
    }

    // S5-F5
    @Transactional
    public Payout applyPromoCode(Long payoutId, Long promoCodeId) {
        Payout payout = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new EntityNotFoundException("Payout not found with id: " + payoutId));

        if (payout.getStatus() != PayoutStatus.PENDING) {
            throw new IllegalStateException("Cannot apply promo code to a completed/cancelled payout");
        }

        PromoCode promoCode = promoCodeRepository.findById(promoCodeId)
                .orElseThrow(() -> new EntityNotFoundException("Promo code not found with id: " + promoCodeId));

        if (!Boolean.TRUE.equals(promoCode.getActive())) {
            throw new IllegalStateException("Promo code is not active");
        }

        if (promoCode.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Promo code has expired");
        }

        if (promoCode.getCurrentUses() >= promoCode.getMaxUses()) {
            throw new IllegalStateException("Promo code has reached its maximum number of uses");
        }

        if (payoutPromoRepository.existsByPayout_IdAndPromoCode_Id(payoutId, promoCodeId)) {
            throw new IllegalStateException("Promo code already applied to this payout");
        }

        double discount;
        if (promoCode.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = payout.getAmount() * promoCode.getDiscountValue() / 100.0;
        } else {
            discount = promoCode.getDiscountValue();
        }
        discount = Math.min(discount, payout.getAmount());

        PayoutPromo payoutPromo = new PayoutPromo();
        payoutPromo.setPayout(payout);
        payoutPromo.setPromoCode(promoCode);
        payoutPromo.setDiscountApplied(discount);
        payoutPromo.setAppliedAt(LocalDateTime.now());
        payoutPromoRepository.save(payoutPromo);

        promoCode.setCurrentUses(promoCode.getCurrentUses() + 1);
        promoCodeRepository.save(promoCode);

        return payoutRepository.findById(payoutId).orElseThrow();
    }
}
