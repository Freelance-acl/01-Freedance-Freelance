package com.team01.freelance.wallet.service;

import com.team01.freelance.contract.repository.ContractRepository;
import com.team01.freelance.contracts.events.ProposalCompletedEvent;
import com.team01.freelance.user.repository.UserRepository;
import com.team01.freelance.wallet.dto.AppliedPromoCodeDTO;
import com.team01.freelance.wallet.dto.CategoryRevenueDTO;
import com.team01.freelance.wallet.dto.PayoutDetailsDTO;
import com.team01.freelance.wallet.dto.PromoCodeUsageDTO;
import com.team01.freelance.wallet.dto.FreelancerPayoutSummaryDTO;
import com.team01.freelance.wallet.dto.RevenueReportDTO;
import com.team01.freelance.wallet.model.Payout;
import com.team01.freelance.wallet.model.PayoutPromo;
import com.team01.freelance.wallet.model.PayoutStatus;
import com.team01.freelance.wallet.messaging.PaymentEventPublisher;
import com.team01.freelance.wallet.dto.MilestoneReversalRequest;
import com.team01.freelance.wallet.dto.ProcessPayoutRequest;
import com.team01.freelance.common.observer.EventSubject;
import com.team01.freelance.wallet.observer.EntityObserver;
import com.team01.freelance.wallet.repository.PayoutAuditEventRepository;
import com.team01.freelance.wallet.strategy.NoReversalStrategy;
import com.team01.freelance.wallet.strategy.RefundResult;
import com.team01.freelance.wallet.strategy.RefundStrategy;
import com.team01.freelance.wallet.strategy.RefundStrategySelector;
import com.team01.freelance.wallet.model.PromoCode;
import com.team01.freelance.wallet.repository.PayoutRepository;
import com.team01.freelance.wallet.repository.PayoutPromoRepository;
import com.team01.freelance.wallet.repository.PromoCodeRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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

    private static final Logger log = LoggerFactory.getLogger(PayoutService.class);

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

    @Autowired(required = false)
    private PaymentEventPublisher paymentEventPublisher;

    private final List<EntityObserver> observers = new ArrayList<>();

    public void registerObserver(EntityObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public void unregisterObserver(EntityObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers(String eventType, Object payload) {
        for (EntityObserver observer : observers) {
            observer.onEvent(eventType, payload);
        }
    }
    @Autowired
    private PayoutAuditEventRepository payoutAuditEventRepository;

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
    @Caching(evict = {
            @CacheEvict(value = "payout", allEntries = true),
            @CacheEvict(value = "S5-F1", allEntries = true),
            @CacheEvict(value = "S5-F3", allEntries = true),
            @CacheEvict(value = "S5-F6", allEntries = true),
            @CacheEvict(value = "S5-F8", allEntries = true),
            @CacheEvict(value = "S5-F10", allEntries = true)
    })
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

    @Caching(evict = {
            @CacheEvict(value = "payout", allEntries = true),
            @CacheEvict(value = "S5-F1", allEntries = true),
            @CacheEvict(value = "S5-F3", allEntries = true),
            @CacheEvict(value = "S5-F6", allEntries = true),
            @CacheEvict(value = "S5-F8", allEntries = true),
            @CacheEvict(value = "S5-F10", allEntries = true)
    })
    public boolean deletePayoutById(Long id) {
        if (!payoutRepository.existsById(id)) {
            return false;
        }
        payoutRepository.deleteById(id);
        return true;
    }

    @Caching(evict = {
            @CacheEvict(value = "payout", allEntries = true),
            @CacheEvict(value = "S5-F1", allEntries = true),
            @CacheEvict(value = "S5-F3", allEntries = true),
            @CacheEvict(value = "S5-F6", allEntries = true),
            @CacheEvict(value = "S5-F8", allEntries = true),
            @CacheEvict(value = "S5-F10", allEntries = true)
    })
    public void deleteAllPayouts() {
        payoutRepository.deleteAll();
    }

    @Caching(evict = {
            @CacheEvict(value = "payout", allEntries = true),
            @CacheEvict(value = "S5-F1", allEntries = true),
            @CacheEvict(value = "S5-F3", allEntries = true),
            @CacheEvict(value = "S5-F6", allEntries = true),
            @CacheEvict(value = "S5-F8", allEntries = true),
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

        int nextAttempt = retryAttempt + 1;
        transactionDetails.put("retryAttempt", nextAttempt);
        transactionDetails.put("gatewayResponse", "approved");

        payout.setStatus(PayoutStatus.COMPLETED);
        payout.setTransactionDetails(transactionDetails);

        Payout saved = payoutRepository.save(payout);

        // Dispatches event to GoF Observers exactly matching PayoutAuditEvent properties
        try {
            Map<String, Object> retryAuditParams = new HashMap<>();
            retryAuditParams.put("payoutId", saved.getId());
            retryAuditParams.put("action", "COMPLETED");
            retryAuditParams.put("method", saved.getMethod()); // Passes PayoutMethod enum directly
            retryAuditParams.put("amount", saved.getAmount()); // Match Double object assignment

            // Populate the document's internal Map<String, Object> details block
            Map<String, Object> innerDetails = new HashMap<>();
            innerDetails.put("retryAttempt", nextAttempt);
            innerDetails.put("gatewayResponse", "approved");
            innerDetails.put("note", "Payout retried successfully after initial failure");

            retryAuditParams.put("details", innerDetails);

            // Notify GoF Observer Chain
            payoutEventSubject.notifyObservers("COMPLETED", retryAuditParams);
        } catch (Exception e) {
            log.warn("Mongo logging failed during retry processing for payout ID {}: {}", saved.getId(), e.getMessage());
        }

        return saved;
    }

    @Cacheable(value = "S5-F8", key = "#payoutId")
    public PayoutDetailsDTO getPayoutDetails(Long payoutId) {

        Payout payout = payoutRepository.findByIdWithPromos(payoutId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Payout not found with id: " + payoutId));

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

        return PayoutDetailsDTO.builder()
                .payoutId(payout.getId())
                .contractId(payout.getContractId())
                .freelancerId(payout.getFreelancerId())
                .originalAmount(payout.getAmount())
                .method(payout.getMethod())
                .status(payout.getStatus())
                .transactionDetails(payout.getTransactionDetails())
                .appliedPromoCodes(promoList)
                .totalDiscount(totalDiscount)
                .finalAmount(payout.getAmount() - totalDiscount)
                .build();
    }

    @Cacheable(value = "S5-F9", key = "#limit")
    public List<PromoCodeUsageDTO> getTopUsedPromoCodes(int limit) {

        List<Object[]> rows = payoutRepository.findTopUsedPromoCodes(limit);
        List<PromoCodeUsageDTO> result = new ArrayList<>();

        if (rows == null) {
            return result;
        }

        for (Object[] r : rows) {
            boolean isExpired = false;
            if (r.length > 7 && r[7] != null) {
                if (r[7] instanceof java.time.LocalDateTime ldt) {
                    isExpired = ldt.isBefore(java.time.LocalDateTime.now());
                } else if (r[7] instanceof java.sql.Timestamp ts) {
                    isExpired = ts.toLocalDateTime().isBefore(java.time.LocalDateTime.now());
                }
            }

            result.add(PromoCodeUsageDTO.builder()
                    .promoCodeId(r[0] != null ? ((Number) r[0]).longValue() : null)
                    .code(r[1] != null ? String.valueOf(r[1]) : null)
                    .discountType(r[2] != null ? String.valueOf(r[2]) : null)
                    .discountValue(r[3] != null ? ((Number) r[3]).doubleValue() : 0.0)
                    .timesUsed(r[4] != null ? ((Number) r[4]).longValue() : 0L)
                    .totalDiscountGiven(r[5] != null ? ((Number) r[5]).doubleValue() : 0.0)
                    .active(r[6] != null ? (Boolean) r[6] : false)
                    .expired(isExpired)
                    .build());
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
    /**
     * [S5-F2] Process a refund on a COMPLETED payout.
     */
    @Caching(evict = {
            @CacheEvict(value = "payout", allEntries = true),
            @CacheEvict(value = "S5-F1", allEntries = true),
            @CacheEvict(value = "S5-F3", allEntries = true),
            @CacheEvict(value = "S5-F6", allEntries = true),
            @CacheEvict(value = "S5-F8", allEntries = true),
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

        Payout savedPayout = payoutRepository.save(payout);

        Map<String, Object> payload = new HashMap<>();
        payload.put("payoutId", savedPayout.getId());
        payload.put("amount", savedPayout.getAmount());
        payload.put("method", savedPayout.getMethod() != null ? savedPayout.getMethod().name() : null);
        payload.put("timestamp", LocalDateTime.now());
        payload.put("details", savedPayout.getTransactionDetails());

        notifyObservers("REFUNDED", payload);

        return savedPayout;
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

    @Transactional
    public Payout handleProposalCompleted(ProposalCompletedEvent event) {
        if (event == null || event.contractId() == null || event.freelancerId() == null || event.agreedAmount() == null) {
            throw new IllegalArgumentException("proposal.completed event is missing payout fields");
        }
        Optional<Payout> existing = payoutRepository.findByContractIdAndStatus(event.contractId(), PayoutStatus.PENDING);
        if (existing.isPresent()) {
            return existing.get();
        }

        payoutRepository.insertPendingPayout(
                event.contractId(),
                event.freelancerId(),
                event.agreedAmount().doubleValue(),
                LocalDateTime.now()
        );
        Payout payout = payoutRepository.findByContractIdAndStatus(event.contractId(), PayoutStatus.PENDING)
                .orElseThrow(() -> new EntityNotFoundException("Pending payout was not created for contract: " + event.contractId()));
        publishAfterCommit(() -> {
            if (paymentEventPublisher != null) {
                paymentEventPublisher.publishPaymentInitiated(payout, event.proposalId());
            }
        });
        return payout;
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
            @CacheEvict(value = "payout", allEntries = true),
            @CacheEvict(value = "S5-F1", allEntries = true),
            @CacheEvict(value = "S5-F3", allEntries = true),
            @CacheEvict(value = "S5-F6", allEntries = true),
            @CacheEvict(value = "S5-F8", allEntries = true),
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

        try {
            Map<String, Object> createdAudit = buildAuditPayload(payout.getId(), payout, null, "CREATED", payout.getAmount());
            payoutEventSubject.notifyObservers("PAYOUT_AUDIT", createdAudit);
        } catch (Exception e) {
            log.warn("Audit event failed for CREATED on contract {}: {}", contractId, e.getMessage());
        }

        Double agreedAmount = payoutRepository.findAgreedAmountByContractId(contractId);
        double platformFee = agreedAmount != null ? agreedAmount * 0.10 : payout.getAmount() * 0.10;

        payout.setMethod(request.getMethod());

        String action = request.isSimulateFailure() ? "FAILED" : "COMPLETED";
        payout.setStatus(request.isSimulateFailure() ? PayoutStatus.FAILED : PayoutStatus.COMPLETED);

        Map<String, Object> txDetails = new LinkedHashMap<>();
        txDetails.put("transactionId", UUID.randomUUID().toString());
        txDetails.put("method", request.getMethod().name());
        if (request.getAccountLastFour() != null) {
            txDetails.put("accountLastFour", request.getAccountLastFour());
        }
        txDetails.put("platformFee", platformFee);
        txDetails.put("processedAt", LocalDateTime.now().toString());
        payout.setTransactionDetails(txDetails);

        Payout saved = payoutRepository.save(payout);

        try {
            Map<String, Object> audit = buildAuditPayload(saved.getId(), saved, null, action, saved.getAmount());
            payoutEventSubject.notifyObservers("PAYOUT_AUDIT", audit);
        } catch (Exception e) {
            log.warn("Audit event failed for {} on contract {}: {}", action, contractId, e.getMessage());
        }

        Long proposalId = findProposalIdForContract(contractId);
        publishAfterCommit(() -> {
            if (paymentEventPublisher == null || proposalId == null) {
                return;
            }
            if (saved.getStatus() == PayoutStatus.FAILED) {
                paymentEventPublisher.publishPaymentFailed(saved, proposalId, "simulated payout failure");
            } else if (saved.getStatus() == PayoutStatus.COMPLETED) {
                paymentEventPublisher.publishPaymentCompleted(saved, proposalId);
            }
        });

        return saved;
    }

    private Long findProposalIdForContract(Long contractId) {
        if (contractId == null) {
            return null;
        }
        return contractRepository.findById(contractId)
                .map(contract -> contract.getProposalId())
                .orElse(null);
    }

    private void publishAfterCommit(Runnable publisher) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publisher.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publisher.run();
            }
        });
    }

    // S5-F5
    @Caching(evict = {
            @CacheEvict(value = "payout", allEntries = true),
            @CacheEvict(value = "S5-F1", allEntries = true),
            @CacheEvict(value = "S5-F3", allEntries = true),
            @CacheEvict(value = "S5-F5", allEntries = true),
            @CacheEvict(value = "S5-F6", allEntries = true),
            @CacheEvict(value = "S5-F8", allEntries = true),
            @CacheEvict(value = "S5-F9", allEntries = true),
            @CacheEvict(value = "S5-F10", allEntries = true),
            @CacheEvict(value = "S5-F11", allEntries = true),
            @CacheEvict(value = "promo-code", allEntries = true)
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

        try {
            Map<String, Object> promoParams = new HashMap<>();
            promoParams.put("payoutId", payoutId);
            promoParams.put("action", "PROMO_APPLIED");
            promoParams.put("method", payout.getMethod());
            promoParams.put("amount", discount);
            Map<String, Object> details = new HashMap<>();
            details.put("promoCodeId", promoCodeId);
            details.put("discountType", promoCode.getDiscountType().name());
            promoParams.put("details", details);
            payoutEventSubject.notifyObservers("PAYOUT_AUDIT", promoParams);
        } catch (Exception e) {
            log.warn("Audit event failed for PROMO_APPLIED on payout {}: {}", payoutId, e.getMessage());
        }

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

        return RevenueReportDTO.builder()
                .totalRevenue(totalRevenue == null ? 0.0 : totalRevenue)
                .totalTransactions(totalTransactions == null ? 0L : totalTransactions)
                .averagePayout(averagePayout)
                .refundedAmount(refundedAmount == null ? 0.0 : refundedAmount)
                .refundCount(refundCount == null ? 0L : refundCount)
                .build();
    }

    // [S5-F10] Category revenue analytics
    @Cacheable(value = "S5-F10", key = "#startDate.toString() + #endDate.toString()")
    public List<CategoryRevenueDTO> getCategoryRevenue(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalStateException("startDate cannot be after endDate");
        }

        payoutEventSubject.notifyObservers("ANALYTICS_VIEWED", Map.of(
                "view", "category-revenue",
                "startDate", startDate.toString(),
                "endDate", endDate.toString()));

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59, 999_999_999);

        List<Object[]> rows = payoutRepository.aggregateCompletedByCategory(start, end);
        List<CategoryRevenueDTO> result = new ArrayList<>();

        for (Object[] row : rows) {
            String category = (String) row[0];
            double platformFee = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            double netPayout   = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
            double total       = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
            long   count       = row[4] != null ? ((Number) row[4]).longValue()   : 0L;

            result.add(CategoryRevenueDTO.builder()
                    .category(category)
                    .platformFeeRevenue(platformFee)
                    .netPayoutRevenue(netPayout)
                    .totalRevenue(total)
                    .payoutCount(count)
                    .build());
        }

        return result;
    }

    // Feature [S5-F11]
    @Cacheable(value = "S5-F11", key = "T(java.util.Objects).hash(#startDate, #endDate)")
    public List<com.team01.freelance.wallet.dto.PayoutMethodDTO> getPayoutMethodBreakdown(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate cannot be after endDate");
        }

        LocalDateTime startInclusive = startDate.atStartOfDay();
        LocalDateTime endInclusive = endDate.atTime(23, 59, 59, 999_999_999);

        List<com.team01.freelance.wallet.event.PayoutAuditEvent> events =
                payoutAuditEventRepository.findByTimestampBetweenAndActionIn(
                        startInclusive,
                        endInclusive,
                        List.of("COMPLETED", "FAILED")
                );

        Map<com.team01.freelance.wallet.model.PayoutMethod, long[]> analyticsMap = new HashMap<>();

        for (com.team01.freelance.wallet.model.PayoutMethod m : com.team01.freelance.wallet.model.PayoutMethod.values()) {
            analyticsMap.put(m, new long[]{0L, 0L});
        }

        Map<com.team01.freelance.wallet.model.PayoutMethod, Double> totalAmountMap = new HashMap<>();

        for (com.team01.freelance.wallet.event.PayoutAuditEvent event : events) {
            com.team01.freelance.wallet.model.PayoutMethod method = event.getMethod();
            if (method == null) continue;

            long[] stats = analyticsMap.get(method);
            if ("COMPLETED".equals(event.getAction())) {
                stats[0]++;
                double currentAmt = totalAmountMap.getOrDefault(method, 0.0);
                totalAmountMap.put(method, currentAmt + (event.getAmount() != null ? event.getAmount() : 0.0));
            } else if ("FAILED".equals(event.getAction())) {
                stats[1]++;
            }
        }

        List<com.team01.freelance.wallet.dto.PayoutMethodDTO> resultList = new ArrayList<>();

        for (com.team01.freelance.wallet.model.PayoutMethod m : com.team01.freelance.wallet.model.PayoutMethod.values()) {
            long[] stats = analyticsMap.get(m);
            long successCount = stats[0];
            long failureCount = stats[1];
            long totalCount = successCount + failureCount;

            if (totalCount == 0) continue;

            double successRate = totalCount > 0 ? (double) successCount / totalCount : 0.0;
            double totalAmount = totalAmountMap.getOrDefault(m, 0.0);

            com.team01.freelance.wallet.dto.PayoutMethodDTO dto = new com.team01.freelance.wallet.dto.PayoutMethodDTO();
            dto.setMethod(m);
            dto.setSuccessCount(successCount);
            dto.setFailureCount(failureCount);
            dto.setSuccessRate(successRate);
            dto.setTotalAmount(totalAmount);

            resultList.add(dto);
        }

        return resultList;
    }

}
