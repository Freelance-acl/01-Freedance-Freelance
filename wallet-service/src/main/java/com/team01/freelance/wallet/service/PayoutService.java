package com.team01.freelance.wallet.service;

import com.team01.freelance.contract.repository.ContractRepository;
import com.team01.freelance.user.repository.UserRepository;
import com.team01.freelance.wallet.dto.FreelancerPayoutSummaryDTO;
import com.team01.freelance.wallet.model.Payout;
import com.team01.freelance.wallet.model.PayoutStatus;
import com.team01.freelance.wallet.dto.ProcessPayoutRequest;
import com.team01.freelance.wallet.model.PayoutStatus;
import com.team01.freelance.wallet.repository.PayoutRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;

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

        // Half-open interval: [startOfStart, startOfDayAfterEnd). Using the
        // day-after midnight as an exclusive upper bound keeps the comparison
        // precision-agnostic regardless of how the JDBC driver handles
        // sub-microsecond fractions.
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endExclusive = endDate != null ? endDate.plusDays(1).atStartOfDay() : null;
        String statusName = status != null ? status.name() : null;
        return payoutRepository.searchByStatusAndCreatedAtRange(statusName, startDateTime, endExclusive);
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

    /**
     * [S5-F3] Build a freelancer payout summary DTO aggregating COMPLETED
     * payouts grouped by method.
     *
     * @param freelancerId the freelancer (user) ID
     * @return summary with totalPayouts, totalAmount and per-method breakdown
     * @throws ResponseStatusException 404 if the user does not exist
     */
    public FreelancerPayoutSummaryDTO getFreelancerPayoutSummary(Long freelancerId) {
        if (!userRepository.existsById(freelancerId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "User not found with id: " + freelancerId);
        }

        List<Object[]> rows = payoutRepository.aggregateCompletedByMethodForFreelancer(freelancerId);

        Map<String, Double> methodBreakdown = new LinkedHashMap<>();
        long totalPayouts = 0L;
        double totalAmount = 0.0;

        for (Object[] row : rows) {
            String method = (String) row[0];
            long count = ((Number) row[1]).longValue();
            double amount = ((Number) row[2]).doubleValue();

            methodBreakdown.put(method, amount);
            totalPayouts += count;
            totalAmount += amount;
        }

        return new FreelancerPayoutSummaryDTO(
                freelancerId,
                totalPayouts,
                totalAmount,
                methodBreakdown
        );
    }

    /**
     * [S5-F2] Process a refund on a COMPLETED payout.
     * Transitions status to REFUNDED and merges refundReason / refundedAt
     * into the JSONB transactionDetails (does not overwrite other keys).
     *
     * @param id the payout ID
     * @param reason the human-readable refund reason
     * @return the updated payout
     * @throws ResponseStatusException 404 if not found, 400 if not COMPLETED
     */
    @Transactional
    public Payout refundPayout(Long id, String reason) {
        Payout payout = payoutRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Payout not found with id: " + id));

        if (payout.getStatus() != PayoutStatus.COMPLETED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only COMPLETED payouts can be refunded (current status: "
                            + payout.getStatus() + ")");
        }

        payout.setStatus(PayoutStatus.REFUNDED);

        Map<String, Object> details = payout.getTransactionDetails() != null
                ? new HashMap<>(payout.getTransactionDetails())
                : new HashMap<>();
        details.put("refundReason", reason);
        details.put("refundedAt", LocalDateTime.now().toString());
        payout.setTransactionDetails(details);

        return payoutRepository.save(payout);
    }

    @Transactional
    public Payout processContractPayout(Long contractId, ProcessPayoutRequest request) {
        if (payoutRepository.countContractById(contractId) == 0) {
            throw new EntityNotFoundException("Contract not found with id: " + contractId);
        }

        String contractStatus = payoutRepository.findContractStatusById(contractId);
        if (!"COMPLETED".equalsIgnoreCase(contractStatus)) {
            throw new IllegalStateException("Contract " + contractId + " is not COMPLETED");
        }

        Payout payout = payoutRepository
                .findByContractIdAndStatusForUpdate(contractId, PayoutStatus.PENDING.name())
                .orElseThrow(() -> {
                    if (payoutRepository.existsByContractIdAndStatus(
                            contractId, PayoutStatus.COMPLETED)) {
                        return new IllegalStateException(
                                "Payout already paid for contract: " + contractId);
                    }
                    return new EntityNotFoundException(
                            "No pending payout found for contract: " + contractId);
                });

        if (request == null || request.getMethod() == null) {
            throw new IllegalArgumentException("Payout method is required");
        }
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
}
