package com.team01.freelance.wallet.service;

import com.team01.freelance.contract.repository.ContractRepository;
import com.team01.freelance.user.repository.UserRepository;
import com.team01.freelance.wallet.model.Payout;
import com.team01.freelance.wallet.model.PayoutStatus;
import com.team01.freelance.wallet.repository.PayoutRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

    public List<Payout> searchPayouts(PayoutStatus status, LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate cannot be after endDate");
        }

        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(LocalTime.MAX) : null;
        String statusName = status != null ? status.name() : null;
        return payoutRepository.searchByStatusAndCreatedAtRange(statusName, startDateTime, endDateTime);
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
}
