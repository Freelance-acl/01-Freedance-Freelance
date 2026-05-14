package com.team01.freelance.wallet.service;

import com.team01.freelance.wallet.dto.RevenueReportDTO;
import com.team01.freelance.wallet.model.Payout;
import com.team01.freelance.wallet.repository.PayoutRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    // S5-F6
    public RevenueReportDTO getRevenueReport(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate must not be after endDate");
        }

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59, 999_999_999);

        Double totalRevenue = payoutRepository.sumCompletedAmountBetween(start, end);
        Long totalTransactions = payoutRepository.countCompletedBetween(start, end);

        double averagePayout = (totalTransactions == null || totalTransactions == 0)
                ? 0.0
                : totalRevenue / totalTransactions;

        Double refundedAmount = payoutRepository.sumRefundedAmountBetween(start, end);
        Long refundCount = payoutRepository.countRefundedBetween(start, end);

        return new RevenueReportDTO(
                totalRevenue == null ? 0.0 : totalRevenue,
                totalTransactions == null ? 0L : totalTransactions,
                averagePayout,
                refundedAmount == null ? 0.0 : refundedAmount,
                refundCount == null ? 0L : refundCount
        );
    }
}
