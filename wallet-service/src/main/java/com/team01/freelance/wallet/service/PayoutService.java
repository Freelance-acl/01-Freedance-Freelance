package com.team01.freelance.wallet.service;

import com.team01.freelance.contract.repository.ContractRepository;
import com.team01.freelance.user.repository.UserRepository;
import com.team01.freelance.wallet.dto.AppliedPromoCodeDTO;
import com.team01.freelance.wallet.dto.PayoutDetailsDTO;
import com.team01.freelance.wallet.dto.PromoCodeUsageDTO;
import com.team01.freelance.wallet.model.Payout;
import com.team01.freelance.wallet.model.PayoutPromo;
import com.team01.freelance.wallet.repository.PayoutRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class PayoutService {

    @Autowired
    private PayoutRepository payoutRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private UserRepository userRepository;

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

        if (contractRepository.findById(payout.getContractId()).isEmpty()){
            throw new EntityNotFoundException("Contract not found with id: " + payout.getContractId());
        }

        if (userRepository.findById(payout.getFreelancerId()).isEmpty()){
            throw new EntityNotFoundException("Freelancer not found with id: " + payout.getFreelancerId());
        }

        return payoutRepository.save(payout);
    }

    /**
     * Updates editable fields on an existing payout.
     * Link fields (contractId, freelancerId) are immutable after creation.
     *
     * @param id The ID of the payout to update
     * @param payoutDetails The object containing updated fields
     * @return The updated payout
     * @throws EntityNotFoundException if the payout is not found
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

    public PayoutDetailsDTO getPayoutDetails(Long payoutId) {

        Payout payout = payoutRepository.findByIdWithPromos(payoutId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Payout not found with id: " + payoutId));

        PayoutDetailsDTO dto = new PayoutDetailsDTO();

        dto.payoutId = payout.getId();
        dto.contractId = payout.getContractId();
        dto.freelancerId = payout.getFreelancerId();

        dto.originalAmount = payout.getAmount();
        dto.method = payout.getMethod();
        dto.status = payout.getStatus();
        dto.transactionDetails = payout.getTransactionDetails();

        List<AppliedPromoCodeDTO> promoList = new ArrayList<>();
        double totalDiscount = 0.0;

        List<PayoutPromo> promos = Optional.ofNullable(payout.getPayoutPromos())
                .orElse(Collections.emptyList());

        for (PayoutPromo pp : promos) {

            AppliedPromoCodeDTO p = new AppliedPromoCodeDTO();

            p.promoCode = pp.getPromoCode().getCode();
            p.discountType = pp.getPromoCode().getDiscountType().toString();
            p.discountApplied = pp.getDiscountApplied();
            p.appliedAt = pp.getAppliedAt();

            totalDiscount += pp.getDiscountApplied();
            promoList.add(p);
        }

        dto.appliedPromoCodes = promoList;
        dto.totalDiscount = totalDiscount;
        dto.finalAmount = dto.originalAmount - totalDiscount;

        return dto;
    }

    public List<PromoCodeUsageDTO> getTopUsedPromoCodes(int limit) {

        List<Object[]> rows = payoutRepository.findTopUsedPromoCodes(limit);

        List<PromoCodeUsageDTO> result = new ArrayList<>();

        for (Object[] r : rows) {

            PromoCodeUsageDTO dto = new PromoCodeUsageDTO();

            dto.promoCodeId = ((Number) r[0]).longValue();
            dto.code = (String) r[1];
            dto.discountType = String.valueOf(r[2]);
            dto.discountValue = r[3] != null ? ((Number) r[3]).doubleValue() : 0;

            dto.timesUsed = r[4] != null ? ((Number) r[4]).intValue() : 0;
            dto.totalDiscountGiven = r[5] != null ? ((Number) r[5]).doubleValue() : 0;

            dto.active = (Boolean) r[6];

            java.sql.Timestamp expiry = (java.sql.Timestamp) r[7];
            dto.expired = expiry != null && expiry.toLocalDateTime().isBefore(java.time.LocalDateTime.now());

            result.add(dto);
        }

        return result;
    }
}
