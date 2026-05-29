package com.team01.freelance.wallet.service;

import com.team01.freelance.contract.repository.ContractRepository;
import com.team01.freelance.user.repository.UserRepository;
import com.team01.freelance.wallet.dto.AppliedPromoCodeDTO;
import com.team01.freelance.wallet.dto.PayoutDetailsDTO;
import com.team01.freelance.wallet.dto.PromoCodeUsageDTO;
import com.team01.freelance.wallet.dto.FreelancerPayoutSummaryDTO;
import com.team01.freelance.wallet.dto.RevenueReportDTO;
import com.team01.freelance.wallet.model.Payout;
import com.team01.freelance.wallet.model.PayoutPromo;
import com.team01.freelance.wallet.model.PayoutStatus;
import com.team01.freelance.wallet.dto.MilestoneReversalRequest;
import com.team01.freelance.wallet.dto.ProcessPayoutRequest;
import com.team01.freelance.common.observer.EventSubject;
import com.team01.freelance.wallet.strategy.NoReversalStrategy;
import com.team01.freelance.wallet.strategy.RefundResult;
import com.team01.freelance.wallet.strategy.RefundStrategy;
import com.team01.freelance.wallet.strategy.RefundStrategySelector;
import com.team01.freelance.wallet.model.PromoCode;
import com.team01.freelance.wallet.repository.PayoutRepository;
import com.team01.freelance.wallet.repository.PayoutPromoRepository;
import com.team01.freelance.wallet.repository.PromoCodeRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import org.springframework.web.server.ResponseStatusException;
import com.team01.freelance.wallet.model.DiscountType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.HashMap;

@Service
public class PayoutService {

    @Autowired
    private PayoutRepository payoutRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PayoutPromoRepository payoutPromoRepository;

    @Autowired
    private PromoCodeRepository promoCodeRepository;

    @Autowired
    private RefundStrategySelector refundStrategySelector;

    @Autowired
    private EventSubject payoutEventSubject;

    public List<Payout> getAllPayouts() {
        return payoutRepository.findAll();
    }

    @Cacheable(value = "payout", key = "#id")
    public Optional<Payout> getPayoutById(Long id) {
        return payoutRepository.findById(id);
    }

    @Cacheable(value = "S5-F1", key = "#status + ':' + #startDate + ':' + #endDate")
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
    @CacheEvict(value = "payout", allEntries = true)
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

    @CacheEvict(value = "payout", allEntries = true)
    public boolean deletePayoutById(Long id) {
        if (!payoutRepository.existsById(id)) {
            return false;
        }
        payoutRepository.deleteById(id);
        return true;
    }

    @CacheEvict(value = "payout", allEntries = true)
    public void deleteAllPayouts() {
        payoutRepository.deleteAll();
    }

    @Caching(evict = {
            @CacheEvict(value = "payout", key = "#id"),
            @CacheEvict(value = "S5-F10", allEntries = true),
            @CacheEvict(value = "S5-F11", allEntries = true)
    })
    @Transactional
    public Payout retryPayout(Long id) {

        Payout payout = payoutRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Payout not found with id: " + id));

        if (payout.getStatus() != PayoutStatus.FAILED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only FAILED payouts can be retried");
        }

        Map<String, Object> transactionDetails = payout.getTransactionDetails();

        if (transactionDetails == null) {
            throw new IllegalArgumentException("Transaction details are missing");
        }

        int retryAttempt = 0;

        Object retryValue = transactionDetails.get("retryAttempt");

        if (retryValue instanceof Number) {
            retryAttempt = ((Number) retryValue).intValue();
        }

        // Hardcoded to COMPLETED as there does not exist payments.

        transactionDetails.put("retryAttempt", retryAttempt + 1);
        transactionDetails.put("gatewayResponse", "approved");

        payout.setStatus(PayoutStatus.COMPLETED);

        payout.setTransactionDetails(transactionDetails);

        return payoutRepository.save(payout);
    }

    @Cacheable(value = "S5-F8", key = "#payoutId")
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
                .filter(list -> !list.isEmpty())
                .orElseGet(() -> payoutPromoRepository.findByPayout_Id(payoutId));
        if (promos == null) {
            promos = Collections.emptyList();
        }

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

    @Cacheable(value = "S5-F9", key = "#limit")
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

            LocalDateTime expiry = (LocalDateTime) r[7];

            dto.expired = expiry != null && expiry.isBefore(LocalDateTime.now());

            result.add(dto);
        }

        return result;
    }

    /**
     * [S5-F3] Build a freelancer payout summary DTO aggregating COMPLETED
     * payouts grouped by method.
     *
     * @param freelancerId the freelancer (user) ID
     * @return summary with totalPayouts, totalAmount and per-method breakdown
     * @throws ResponseStatusException 404 if the user does not exist
     */
    @Cacheable(value = "S5-F3", key = "#freelancerId")
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
    @Caching(evict = {
            @CacheEvict(value = "payout", key = "#id"),
            @CacheEvict(value = "S5-F10", allEntries = true),
            @CacheEvict(value = "S5-F11", allEntries = true)
    })
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

    /**
     * [S5-F12] Reverse a milestone-based payout using DP-1 strategy selection.
     */
    @Transactional
    public Payout reverseMilestone(Long id, MilestoneReversalRequest request) {
        System.out.println("Reverse Milestone Payout");
        Payout payout = payoutRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Payout not found with id: " + id));

        if (payout.getStatus() != PayoutStatus.COMPLETED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only COMPLETED payouts can be reversed (current status: "
                            + payout.getStatus() + ")");
        }

        RefundStrategy strategy = refundStrategySelector.select(payout, request);

        if (strategy instanceof NoReversalStrategy) {
            Map<String, Object> deniedAudit = buildAuditPayload(
                    id, payout, request, "REFUND_DENIED", 0.0);
            Map<String, Object> deniedDetails = castDetails(deniedAudit);
            deniedDetails.put("strategy", "NoReversalStrategy");
            deniedDetails.put("denialReason", "reversal window expired");
            payoutEventSubject.notifyObservers("PAYOUT_AUDIT", deniedAudit);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "reversal window expired");
        }

        RefundResult result = strategy.calculateRefund(payout, request);
        double refundAmount = result.amount();
        String reversalScope = request != null ? request.getReversalScope() : null;
        String refundReason = request != null ? request.getReason() : null;
        String refundedAt = LocalDateTime.now().toString();

        payout.setStatus(PayoutStatus.REFUNDED);
        Map<String, Object> txDetails = payout.getTransactionDetails() != null
                ? new HashMap<>(payout.getTransactionDetails())
                : new HashMap<>();
        txDetails.put("refundAmount", refundAmount);
        txDetails.put("reversalScope", reversalScope);
        txDetails.put("refundReason", refundReason);
        txDetails.put("refundedAt", refundedAt);
        payout.setTransactionDetails(txDetails);

        Payout saved = payoutRepository.save(payout);

        Map<String, Object> refundedAudit = buildAuditPayload(
                id, payout, request, "REFUNDED", refundAmount);
        Map<String, Object> refundedDetails = castDetails(refundedAudit);
        refundedDetails.put("strategy", result.strategyName());
        refundedDetails.put("reason", refundReason);
        refundedDetails.put("originalAmount", payout.getAmount());
        refundedDetails.put("reversalAmount", refundAmount);
        refundedDetails.put("reversalScope", reversalScope);
        payoutEventSubject.notifyObservers("PAYOUT_AUDIT", refundedAudit);

        return saved;
    }

    private static Map<String, Object> buildAuditPayload(
            Long payoutId,
            Payout payout,
            MilestoneReversalRequest request,
            String action,
            double amount) {
        Map<String, Object> auditParams = new HashMap<>();
        auditParams.put("payoutId", payoutId);
        auditParams.put("method", payout.getMethod());
        auditParams.put("action", action);
        auditParams.put("amount", amount);
        Map<String, Object> details = new HashMap<>();
        if (request != null && request.getReason() != null) {
            details.put("reason", request.getReason());
        }
        if (request != null && request.getReversalScope() != null) {
            details.put("reversalScope", request.getReversalScope());
        }
        auditParams.put("details", details);
        return auditParams;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castDetails(Map<String, Object> auditParams) {
        return (Map<String, Object>) auditParams.get("details");
    }

    // [S5-F4] Process a payout for a COMPLETED contract.
    /**
     * [S5-F4] Process a payout for a COMPLETED contract.
     *
     * @param contractId the contract ID
     * @param request the payout request body containing the method and account last four
     * @return the updated payout
     * @throws EntityNotFoundException if the contract is not found
     * @throws IllegalStateException if the contract is not COMPLETED
     * @throws IllegalArgumentException if the payout method is not provided
     */
    @Caching(evict = {
            @CacheEvict(value = "payout", key = "#contractId"),
            @CacheEvict(value = "S5-F10", allEntries = true),
            @CacheEvict(value = "S5-F11", allEntries = true)
    })
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

    // S5-F5
    @Caching(evict = {
            @CacheEvict(value = "payout", key = "#payoutId"),
            @CacheEvict(value = "S5-F5", allEntries = true),
            @CacheEvict(value = "S5-F10", allEntries = true),
            @CacheEvict(value = "S5-F11", allEntries = true)
    })
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

        if (payoutPromoRepository.existsByPayoutIdAndPromoCodeId(payoutId, promoCodeId)) {
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

    // S5-F6
    @Cacheable(value = "S5-F6", key = "#startDate + ':' + #endDate")
    public RevenueReportDTO getRevenueReport(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalStateException("startDate cannot be after endDate");
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
