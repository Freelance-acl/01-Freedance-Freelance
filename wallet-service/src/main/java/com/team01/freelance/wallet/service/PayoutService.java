package com.team01.freelance.wallet.service;

import com.team01.freelance.wallet.model.Payout;
import com.team01.freelance.wallet.repository.PayoutRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PayoutService {

    @Autowired
    private PayoutRepository payoutRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Payout> getAllPayouts() {
        return payoutRepository.findAll();
    }

    public Optional<Payout> getPayoutById(Long id) {
        return payoutRepository.findById(id);
    }

    public Payout createPayout(Payout payout) {
        validatePayoutReferences(payout.getContractId(), payout.getFreelancerId());
        return payoutRepository.save(payout);
    }

    /**
     * Updates an existing payout and throws if it does not exist.
     * Note: Cross-service foreign keys (contractId, freelancerId) are updated directly.
     *
     * @param id The ID of the payout to update
     * @param payoutDetails The object containing updated fields
     * @return The updated payout
     * @throws RuntimeException if the payout is not found
     */
    public Payout updatePayout(Long id, Payout payoutDetails) {
        return payoutRepository.findById(id).map(existingPayout -> {
                if (payoutDetails.getContractId() != null) existingPayout.setContractId(payoutDetails.getContractId());
                if (payoutDetails.getFreelancerId() != null) existingPayout.setFreelancerId(payoutDetails.getFreelancerId());
                if (payoutDetails.getAmount() != null) existingPayout.setAmount(payoutDetails.getAmount());
                if (payoutDetails.getMethod() != null) existingPayout.setMethod(payoutDetails.getMethod());
                if (payoutDetails.getStatus() != null) existingPayout.setStatus(payoutDetails.getStatus());
                if (payoutDetails.getTransactionDetails() != null) existingPayout.setTransactionDetails(payoutDetails.getTransactionDetails());
            validatePayoutReferences(existingPayout.getContractId(), existingPayout.getFreelancerId());
            return payoutRepository.save(existingPayout);
        }).orElseThrow(() -> new RuntimeException("Payout not found with id: " + id));
    }

    private void validatePayoutReferences(Long contractId, Long freelancerId) {
        validateReferenceExists("contracts", contractId, "Contract");
        validateReferenceExists("users", freelancerId, "Freelancer");
    }

    private void validateReferenceExists(String tableName, Long id, String label) {
        if (id == null) {
            throw new IllegalArgumentException(label + " id is required");
        }

        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM " + tableName + " WHERE id = ?)",
                Boolean.class,
                id
        );

        if (!Boolean.TRUE.equals(exists)) {
            throw new IllegalArgumentException(label + " not found with id: " + id);
        }
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
