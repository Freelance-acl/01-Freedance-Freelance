package com.team01.freelance.wallet.service;

import com.team01.freelance.wallet.model.Payout;
import com.team01.freelance.wallet.repository.PayoutRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PayoutService {

    @Autowired
    private PayoutRepository payoutRepository;

    public List<Payout> getAllPayouts() {
        return payoutRepository.findAll();
    }

    public Optional<Payout> getPayoutById(Long id) {
        return payoutRepository.findById(id);
    }

    public Payout createPayout(Payout payout) {
        return payoutRepository.save(payout);
    }

    public Optional<Payout> updatePayout(Long id, Payout payoutDetails) {
        return payoutRepository.findById(id).map(existingPayout -> {
            if (payoutDetails.getContractId() != null) {
                existingPayout.setContractId(payoutDetails.getContractId());
            }
            if (payoutDetails.getFreelancerId() != null) {
                existingPayout.setFreelancerId(payoutDetails.getFreelancerId());
            }
            if (payoutDetails.getAmount() != null) {
                existingPayout.setAmount(payoutDetails.getAmount());
            }
            if (payoutDetails.getMethod() != null) {
                existingPayout.setMethod(payoutDetails.getMethod());
            }
            if (payoutDetails.getStatus() != null) {
                existingPayout.setStatus(payoutDetails.getStatus());
            }
            if (payoutDetails.getTransactionDetails() != null) {
                existingPayout.setTransactionDetails(payoutDetails.getTransactionDetails());
            }
            return payoutRepository.save(existingPayout);
        });
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
}
