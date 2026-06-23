package com.team01.freelance.proposal.service;

import com.team01.freelance.proposal.dto.FeeEstimateDTO;
import com.team01.freelance.proposal.dto.ProposalAnalyticsDashboardDTO;
import com.team01.freelance.proposal.graph.InteractionGraphService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.team01.freelance.common.observer.EventSubject;
import com.team01.freelance.contracts.events.ContractCreatedEvent;
import com.team01.freelance.contracts.events.ContractStatusChangedEvent;
import com.team01.freelance.contracts.events.PaymentCompletedEvent;
import com.team01.freelance.contracts.events.PaymentFailedEvent;
import com.team01.freelance.contracts.events.PaymentInitiatedEvent;
import com.team01.freelance.contracts.events.PaymentRefundedEvent;
import com.team01.freelance.proposal.dto.FeignJobDTO;
import com.team01.freelance.proposal.dto.FeignUserDTO;
import com.team01.freelance.proposal.dto.JobProposalSummaryByJobDTO;
import com.team01.freelance.proposal.dto.JobProposalSummaryDTO;
import com.team01.freelance.proposal.dto.ProposalAnalyticsDTO;
import com.team01.freelance.proposal.dto.ProposalDetailsDTO;
import com.team01.freelance.proposal.feign.JobServiceClient;
import com.team01.freelance.proposal.feign.UserServiceClient;
import com.team01.freelance.proposal.exception.ForbiddenOperationException;
import com.team01.freelance.proposal.messaging.ProposalEventPublisher;
import com.team01.freelance.proposal.model.MilestoneStatus;
import com.team01.freelance.proposal.model.Proposal;
import com.team01.freelance.proposal.model.ProposalMilestone;
import com.team01.freelance.proposal.model.ProposalStatus;
import com.team01.freelance.proposal.repository.ProposalAnalyticsProjection;
import com.team01.freelance.proposal.repository.ProposalRepository;
import com.team01.freelance.proposal.saga.ProposalStateMachine;
import com.team01.freelance.proposal.saga.SagaTriggerService;
import com.team01.freelance.proposal.security.ProposalAuthSupport;

import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@Service
public class ProposalService {

    private static final Logger log = LoggerFactory.getLogger(ProposalService.class);

    private static final Set<ProposalStatus> ACCEPTABLE_STATUSES = EnumSet.of(
            ProposalStatus.SUBMITTED,
            ProposalStatus.SHORTLISTED
    );
    private static final List<ProposalStatus> MILESTONE_ALLOWED_STATUSES = List.of(
            ProposalStatus.SUBMITTED,
            ProposalStatus.SHORTLISTED);
    private static final List<ProposalStatus> WITHDRAWABLE_STATUSES = List.of(
            ProposalStatus.SUBMITTED,
            ProposalStatus.SHORTLISTED);

    @Autowired
    private ProposalRepository proposalRepository;

    @Autowired
    private EventSubject proposalEventSubject;

    @Autowired
    private InteractionGraphService interactionGraphService;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ProposalAnalyticsCacheService proposalAnalyticsCacheService;

    @Autowired
    private ProposalStateMachine proposalStateMachine;

    @Autowired
    private SagaTriggerService sagaTriggerService;

    @Autowired
    private UserServiceClient userServiceClient;

    @Autowired
    private JobServiceClient jobServiceClient;

    @Autowired
    private ProposalEventPublisher proposalEventPublisher;

    @Autowired
    private com.team01.freelance.proposal.saga.SagaMetrics sagaMetrics;

    @Autowired
    private ProposalAuthSupport proposalAuthSupport;

    public List<Proposal> getAllProposals() {
        return proposalRepository.findAll();
    }

    public Optional<Proposal> getProposalById(Long id) {
        return proposalRepository.findById(id);
    }

    public ProposalDetailsDTO getProposalDetails(Long proposalId) {
        Proposal proposal = proposalRepository.findByIdWithMilestones(proposalId)
                .orElseThrow(() -> new EntityNotFoundException("Proposal not found with id: " + proposalId));
        List<ProposalDetailsDTO.MilestoneDTO> milestones = Optional.ofNullable(proposal.getProposalMilestones())
                .orElseGet(List::of)
                .stream()
                .sorted(Comparator.comparing(ProposalMilestone::getMilestoneOrder))
                .map(this::toMilestoneDTO)
                .toList();

        ProposalDetailsDTO details = new ProposalDetailsDTO();
        details.setProposalId(proposal.getId());
        details.setJobId(proposal.getJobId());
        details.setFreelancerId(proposal.getFreelancerId());
        details.setStatus(proposal.getStatus());
        details.setBidAmount(proposal.getBidAmount());
        details.setMetadata(proposal.getMetadata());
        details.setMilestones(milestones);
        details.setTotalMilestones(milestones.size());
        details.setCompletedMilestones((int) milestones.stream()
                .filter(milestone -> milestone.getStatus() == MilestoneStatus.COMPLETED
                        || milestone.getStatus() == MilestoneStatus.APPROVED)
                .count());
        return details;
    }

    /**
     * Finds proposals whose {@code submittedAt} falls on or after {@code startDate} and on or before {@code endDate}
     * (inclusive calendar days), optionally filtered by status. Results are ordered by {@code submittedAt} descending.
     *
     * @param status optional status filter; null or blank means any status
     */
    public List<Proposal> searchProposals(String status, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("startDate and endDate are required");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate must be on or before endDate");
        }
        ProposalStatus parsedStatus = null;
        if (status != null && !status.isBlank()) {
            parsedStatus = ProposalStatus.fromString(status.trim());
        }
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime endExclusive = endDate.plusDays(1).atStartOfDay();
        return proposalRepository.searchBySubmittedAtRangeAndOptionalStatus(start, endExclusive, parsedStatus);
    }

    /**
     * [S3-F5] Returns proposals whose JSONB metadata field equals the given value for the key.
     * Finds proposals whose metadata JSON contains the given key with the given string value.
     *
     * @param key metadata field name (required, non-blank)
     * @param value metadata field value to match (required, non-blank)
     * @return proposals matching the metadata key/value pair
     * @throws IllegalArgumentException if key or value is null or blank
     */
    public List<Proposal> searchProposalsByMetadata(String key, String value) {
        if (key == null || key.isBlank() || value == null || value.isBlank()) {
            throw new IllegalArgumentException("key and value are required");
        }
        String normalizedKey = key.trim();
        String normalizedValue = value.trim();
        List<Proposal> matches = findProposalsByMetadata(normalizedKey, normalizedValue);
        matches.forEach(proposal -> proposal.setProposalMilestones(new ArrayList<>()));
        return matches;
    }

    private List<Proposal> findProposalsByMetadata(String key, String value) {
        if (!usesPostgresDatabase()) {
            return filterProposalsByMetadata(key, value);
        }
        try {
            List<Long> ids = proposalRepository.findIdsByMetadataEquals(key, value);
            if (ids.isEmpty()) {
                return List.of();
            }
            return proposalRepository.findAllById(ids).stream()
                    .sorted(Comparator.comparing(Proposal::getId))
                    .toList();
        } catch (DataAccessException ex) {
            return filterProposalsByMetadata(key, value);
        }
    }

    private boolean usesPostgresDatabase() {
        try (var connection = dataSource.getConnection()) {
            return "PostgreSQL".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName());
        } catch (Exception ex) {
            return false;
        }
    }

    private List<Proposal> filterProposalsByMetadata(String key, String value) {
        return proposalRepository.findAll().stream()
                .filter(proposal -> proposal.getMetadata() != null
                        && value.equals(String.valueOf(proposal.getMetadata().get(key))))
                .sorted(Comparator.comparing(Proposal::getId))
                .toList();
    }

    public ProposalAnalyticsDashboardDTO getProposalAnalyticsDashboard(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        proposalEventSubject.notifyObservers("ANALYTICS_VIEWED", Map.of(
                "proposalId", -1L,
                "view", "proposal-analytics"));
        return proposalAnalyticsCacheService.getAnalyticsDashboard(startDate, endDate);
    }

    public void recordProposalInteraction(Long proposalId) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new EntityNotFoundException("Proposal not found with id: " + proposalId));

        if (proposal.getStatus() != ProposalStatus.SUBMITTED) {
            throw new IllegalArgumentException("Interaction can only be recorded for SUBMITTED proposals");
        }

        FeignUserDTO freelancer = getUserForEnrichment(proposal.getFreelancerId());
        FeignJobDTO job = getJobForEnrichment(proposal.getJobId());

        interactionGraphService.recordInteraction(
                proposalId,
                proposal.getFreelancerId(),
                freelancer.getName(),
                proposal.getJobId(),
                job.getTitle(),
                job.getCategory() != null ? job.getCategory() : "UNKNOWN"
        );

        proposalEventSubject.notifyObservers("INTERACTION_RECORDED", Map.of(
                "proposalId", proposalId,
                "freelancerId", proposal.getFreelancerId(),
                "jobId", proposal.getJobId(),
                "timestamp", LocalDateTime.now()
        ));
    }

    public ProposalAnalyticsDTO getProposalAnalytics(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime endExclusive = endDate.plusDays(1).atStartOfDay();
        ProposalAnalyticsProjection analytics = proposalRepository.calculateAnalyticsBySubmittedAtRange(start, endExclusive);

        return new ProposalAnalyticsDTO(
                analytics.getTotalProposals().longValue(),
                analytics.getAcceptedProposals().longValue(),
                analytics.getRejectedProposals().longValue(),
                analytics.getTotalBidValue().doubleValue(),
                analytics.getAverageBid().doubleValue(),
                analytics.getAcceptanceRate().doubleValue());
    }

    public Proposal createProposal(Proposal proposal) {
        if (proposal.getFreelancerId() == null || proposal.getJobId() == null) {
            throw new IllegalArgumentException("Freelancer and Job IDs are required to create a Proposal");
        }

        getJobForEnrichment(proposal.getJobId());
        getUserForEnrichment(proposal.getFreelancerId());

        return proposalRepository.save(proposal);
    }

    /**
     * Updates an existing proposal and throws if it does not exist.
     *
     * @param id The ID of the proposal to update
     * @param proposalDetails The object containing updated fields
     * @return The updated proposal
     * @throws EntityNotFoundException if the proposal is not found
     */
    public Proposal updateProposal(Long id, Proposal proposalDetails) {
        return proposalRepository.findById(id).map(existingProposal -> {
            if (proposalDetails.getCoverLetter() != null) existingProposal.setCoverLetter(proposalDetails.getCoverLetter());
            if (proposalDetails.getBidAmount() != null) existingProposal.setBidAmount(proposalDetails.getBidAmount());
            if (proposalDetails.getEstimatedDays() != null) existingProposal.setEstimatedDays(proposalDetails.getEstimatedDays());
            if (proposalDetails.getStatus() != null) existingProposal.setStatus(proposalDetails.getStatus());
            if (proposalDetails.getMetadata() != null) existingProposal.setMetadata(proposalDetails.getMetadata());
            if (proposalDetails.getAcceptedAt() != null) existingProposal.setAcceptedAt(proposalDetails.getAcceptedAt());
            return proposalRepository.save(existingProposal);
        }).orElseThrow(() -> new EntityNotFoundException("Proposal not found with id: " + id));
    }

    @Transactional
    public Proposal acceptProposal(Long id) {
        Proposal proposal = proposalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Proposal not found with id: " + id));

        if (!ACCEPTABLE_STATUSES.contains(proposal.getStatus())) {
            throw new IllegalArgumentException("Only SUBMITTED or SHORTLISTED proposals can be accepted");
        }

        FeignUserDTO freelancer = getFreelancerForAccept(proposal.getFreelancerId());
        if (!"FREELANCER".equalsIgnoreCase(freelancer.getRole())) {
            throw new IllegalArgumentException("User is not a freelancer");
        }

        LocalDateTime now = LocalDateTime.now();
        ProposalStatus oldStatus = proposal.getStatus();
        proposal.setStatus(ProposalStatus.ACCEPTED);
        proposal.setAcceptedAt(now);
        Proposal acceptedProposal = proposalRepository.save(proposal);

        logProposalTransition(proposal.getId(), oldStatus, ProposalStatus.ACCEPTED);
        if (proposalEventPublisher != null) {
            proposalEventPublisher.publishProposalAccepted(acceptedProposal);
        }

        return acceptedProposal;
    }

    @Transactional
    public Proposal addMilestones(Long proposalId, List<ProposalMilestone> milestones) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new EntityNotFoundException("Proposal not found with id: " + proposalId));

        if (!MILESTONE_ALLOWED_STATUSES.contains(proposal.getStatus())) {
            throw new IllegalArgumentException("Milestones can only be added to SUBMITTED or SHORTLISTED proposals");
        }
        if (milestones == null || milestones.isEmpty()) {
            throw new IllegalArgumentException("At least one milestone is required");
        }

        List<ProposalMilestone> existingMilestones = proposal.getProposalMilestonesForUpdate();
        double existingTotal = existingMilestones.stream()
                .map(ProposalMilestone::getAmount)
                .filter(amount -> amount != null)
                .mapToDouble(Double::doubleValue)
                .sum();
        double newTotal = milestones.stream()
                .peek(this::validateMilestone)
                .mapToDouble(ProposalMilestone::getAmount)
                .sum();

        if (proposal.getBidAmount() == null || existingTotal + newTotal > proposal.getBidAmount()) {
            throw new IllegalArgumentException("Total milestone amounts cannot exceed proposal bidAmount");
        }

        int nextOrder = existingMilestones.stream()
                .map(ProposalMilestone::getMilestoneOrder)
                .filter(order -> order != null)
                .max(Integer::compareTo)
                .orElse(0) + 1;

        for (ProposalMilestone milestone : milestones) {
            milestone.setId(null);
            milestone.setMilestoneOrder(nextOrder++);
            milestone.setStatus(MilestoneStatus.PENDING);
            proposal.addProposalMilestone(milestone);
        }

        Proposal saved = proposalRepository.save(proposal);
        proposalEventSubject.notifyObservers("PROPOSAL_MILESTONES_ADDED", Map.of(
                "proposalId", proposalId,
                "action", "PROPOSAL_MILESTONES_ADDED"
        ));
        return saved;
    }

    private void validateMilestone(ProposalMilestone milestone) {
        if (milestone == null) {
            throw new IllegalArgumentException("Milestone is required");
        }
        if (milestone.getTitle() == null || milestone.getTitle().isBlank()) {
            throw new IllegalArgumentException("Milestone title is required");
        }
        if (milestone.getDescription() == null || milestone.getDescription().isBlank()) {
            throw new IllegalArgumentException("Milestone description is required");
        }
        if (milestone.getAmount() == null || milestone.getAmount() <= 0) {
            throw new IllegalArgumentException("Milestone amount must be positive");
        }
    }

    @Transactional
    public Proposal withdrawProposal(Long id) {
        return withdrawProposal(id, null);
    }

    @Transactional
    public Proposal withdrawProposal(Long id, HttpServletRequest request) {
        Proposal proposal = proposalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Proposal not found with id: " + id));

        if (!WITHDRAWABLE_STATUSES.contains(proposal.getStatus())) {
            throw new IllegalArgumentException("Only SUBMITTED or SHORTLISTED proposals can be withdrawn");
        }

        assertProposalOwnerOrAdmin(proposal, request);

        ProposalStatus oldStatus = proposal.getStatus();
        proposal.setStatus(ProposalStatus.WITHDRAWN);
        Proposal withdrawnProposal = proposalRepository.save(proposal);

        logProposalTransition(proposal.getId(), oldStatus, ProposalStatus.WITHDRAWN);
        if (proposalEventPublisher != null) {
            proposalEventPublisher.publishProposalWithdrawn(withdrawnProposal);
        }

        return withdrawnProposal;
    }

    /**
     * Completes work for an accepted proposal via the M3 saga trigger.
     */
    @Transactional
    public Proposal completeProposal(Long id, HttpServletRequest request) {
        Proposal proposal = proposalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Proposal not found with id: " + id));
        assertProposalOwnerOrAdmin(proposal, request);
        return sagaTriggerService.triggerCompletion(id);
    }

    @Transactional
    public Proposal handleContractCreated(ContractCreatedEvent event) {
        if (event == null || event.proposalId() == null) {
            throw new IllegalArgumentException("contract.created event is missing proposalId");
        }
        Proposal proposal = proposalRepository.findById(event.proposalId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Proposal not found with id: " + event.proposalId()));
        if (event.contractId() != null
                && event.contractId().equals(proposal.getContractId())) {
            return proposal;
        }
        proposal.setContractId(event.contractId());
        return proposalRepository.saveAndFlush(proposal);
    }

    @Transactional
    public Proposal handleContractStatusChanged(ContractStatusChangedEvent event) {
        if (event == null || event.contractId() == null) {
            throw new IllegalArgumentException("contract.status-changed event is missing contractId");
        }
        return proposalRepository.findByContractId(event.contractId())
                .map(proposal -> {
                    if (event.contractId().equals(proposal.getContractId())) {
                        return proposal;
                    }
                    proposal.setContractId(event.contractId());
                    return proposalRepository.saveAndFlush(proposal);
                })
                .orElseGet(() -> {
                    log.debug("No proposal linked to contractId {} for contract.status-changed", event.contractId());
                    return null;
                });
    }

    public boolean deleteProposalById(Long id) {
        if (proposalRepository.findById(id).isEmpty()) {
            return false;
        }
        proposalRepository.deleteById(id);
        proposalEventSubject.notifyObservers("PROPOSAL_DELETED", Map.of(
                "proposalId", id,
                "action", "PROPOSAL_DELETED"
        ));
        return true;
    }

    public void deleteAllProposals() {
        proposalRepository.deleteAll();
    }

    /**
     * Computes a read-only platform fee estimate from bid amount and duration.
     *
     * @param bidAmount the proposed bid (must be positive)
     * @param estimatedDays expected delivery days (must be positive)
     * @return fee breakdown for the freelancer
     * @throws IllegalArgumentException if inputs are null or not positive
     */
    public FeeEstimateDTO estimatePlatformFee(Double bidAmount, Integer estimatedDays) {
        if (bidAmount == null || bidAmount <= 0) {
            throw new IllegalArgumentException("bidAmount must be positive");
        }
        if (estimatedDays == null || estimatedDays <= 0) {
            throw new IllegalArgumentException("estimatedDays must be positive");
        }

        double minBid = bidAmount * 0.8;
        double maxBid = bidAmount * 1.2;
        long similarProposalCount = proposalRepository.countActiveProposalsInSimilarBidRange(minBid, maxBid);
        int feePercentage = resolveFeePercentage(similarProposalCount);

        double platformFee = bidAmount * feePercentage / 100.0;
        double freelancerPayout = bidAmount - platformFee;
        double estimatedDailyRate = freelancerPayout / estimatedDays;

        return new FeeEstimateDTO(
                bidAmount,
                platformFee,
                freelancerPayout,
                feePercentage,
                estimatedDailyRate);
    }

    private static int resolveFeePercentage(long similarProposalCount) {
        if (similarProposalCount <= 5) {
            return 20;
        }
        if (similarProposalCount <= 15) {
            return 15;
        }
        return 10;
    }

    private ProposalDetailsDTO.MilestoneDTO toMilestoneDTO(ProposalMilestone proposalMilestone) {
        ProposalDetailsDTO.MilestoneDTO dto = new ProposalDetailsDTO.MilestoneDTO();
        dto.setId(proposalMilestone.getId());
        dto.setMilestoneOrder(proposalMilestone.getMilestoneOrder());
        dto.setTitle(proposalMilestone.getTitle());
        dto.setDescription(proposalMilestone.getDescription());
        dto.setAmount(proposalMilestone.getAmount());
        dto.setStatus(proposalMilestone.getStatus());
        dto.setMetadata(proposalMilestone.getMetadata());
        return dto;
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("startDate and endDate are required");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate must be on or before endDate");
        }
    }

    public JobProposalSummaryDTO getJobProposalSummary(Long jobId, LocalDate startDate, LocalDate endDate) {
        getJobForEnrichment(jobId);

        LocalDateTime queryStart = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime queryEndExclusive = endDate == null ? null : endDate.plusDays(1).atStartOfDay();
        if (startDate != null && endDate != null) {
            validateDateRange(startDate, endDate);
        }

        List<Object[]> rows = proposalRepository.getJobProposalSummary(jobId, queryStart, queryEndExclusive);
        Object[] row = rows.isEmpty() ? new Object[]{0L, 0L, 0.0, 0.0, 0.0} : rows.get(0);
        return new JobProposalSummaryDTO(
                row[0] != null ? ((Number) row[0]).longValue() : 0L,
                row[1] != null ? ((Number) row[1]).longValue() : 0L,
                row[2] != null ? ((Number) row[2]).doubleValue() : 0.0,
                row[3] != null ? ((Number) row[3]).doubleValue() : 0.0,
                row[4] != null ? ((Number) row[4]).doubleValue() : 0.0
        );
    }

    public int getActiveProposalCountForJob(Long jobId) {
        return proposalRepository.countActiveProposalsByJobId(jobId);
    }

    public List<JobProposalSummaryByJobDTO> getJobProposalSummaries(List<Long> jobIds) {
        if (jobIds == null || jobIds.isEmpty()) {
            return List.of();
        }
        return proposalRepository.getJobProposalSummaries(jobIds).stream()
                .map(this::toJobProposalSummaryByJobDTO)
                .toList();
    }

    @Transactional
    public Proposal handlePaymentInitiated(PaymentInitiatedEvent event) {
        Proposal proposal = findProposalFromPaymentEvent(event == null ? null : event.proposalId());
        if (proposal.getStatus() == ProposalStatus.PAYMENT_PENDING
                || proposal.getStatus() == ProposalStatus.PAID
                || proposal.getStatus() == ProposalStatus.REFUNDED) {
            return proposal;
        }
        if (!proposalStateMachine.canTransition(proposal.getStatus(), ProposalStatus.PAYMENT_PENDING)) {
            return proposal;
        }
        proposalStateMachine.transition(proposal, ProposalStatus.PAYMENT_PENDING);
        if (event.contractId() != null && proposal.getContractId() == null) {
            proposal.setContractId(event.contractId());
        }
        return proposalRepository.saveAndFlush(proposal);
    }

    @Transactional
    public Proposal handlePaymentCompleted(PaymentCompletedEvent event) {
        Proposal proposal = findProposalFromPaymentEvent(event == null ? null : event.proposalId());
        if (proposal.getStatus() == ProposalStatus.PAID) {
            return proposal;
        }
        if (!proposalStateMachine.canTransition(proposal.getStatus(), ProposalStatus.PAID)) {
            return proposal;
        }
        proposalStateMachine.transition(proposal, ProposalStatus.PAID);
        sagaMetrics.recordProposalCompleted();
        return proposalRepository.saveAndFlush(proposal);
    }

    @Transactional
    public Proposal handlePaymentFailed(PaymentFailedEvent event) {
        Proposal proposal = findProposalFromPaymentEvent(event == null ? null : event.proposalId());
        if (proposal.getStatus() == ProposalStatus.PAYMENT_FAILED
                || proposal.getStatus() == ProposalStatus.REFUNDED) {
            return proposal;
        }
        if (!proposalStateMachine.canTransition(proposal.getStatus(), ProposalStatus.PAYMENT_FAILED)) {
            return proposal;
        }
        proposalStateMachine.transition(proposal, ProposalStatus.PAYMENT_FAILED);
        Proposal saved = proposalRepository.saveAndFlush(proposal);
        if (proposalEventPublisher != null) {
            String reason = event.reason() != null ? event.reason() : "payment_failed";
            proposalEventPublisher.publishProposalCancelled(saved, reason);
        }
        return saved;
    }

    @Transactional
    public Proposal handlePaymentRefunded(PaymentRefundedEvent event) {
        Proposal proposal = findProposalFromPaymentEvent(event == null ? null : event.proposalId());
        if (proposal.getStatus() == ProposalStatus.REFUNDED) {
            return proposal;
        }
        if (!proposalStateMachine.canTransition(proposal.getStatus(), ProposalStatus.REFUNDED)) {
            return proposal;
        }
        proposalStateMachine.transition(proposal, ProposalStatus.REFUNDED);
        return proposalRepository.saveAndFlush(proposal);
    }

    private JobProposalSummaryByJobDTO toJobProposalSummaryByJobDTO(Object[] row) {
        return new JobProposalSummaryByJobDTO(
                row[0] != null ? ((Number) row[0]).longValue() : null,
                row[1] != null ? ((Number) row[1]).longValue() : 0L,
                row[2] != null ? ((Number) row[2]).longValue() : 0L,
                row[3] != null ? ((Number) row[3]).doubleValue() : 0.0,
                row[4] != null ? ((Number) row[4]).doubleValue() : 0.0,
                row[5] != null ? ((Number) row[5]).doubleValue() : 0.0
        );
    }

    private Proposal findProposalFromPaymentEvent(Long proposalId) {
        if (proposalId == null) {
            throw new IllegalArgumentException("payment event is missing proposalId");
        }
        return proposalRepository.findById(proposalId)
                .orElseThrow(() -> new EntityNotFoundException("Proposal not found with id: " + proposalId));
    }

    private FeignUserDTO getFreelancerForAccept(Long freelancerId) {
        return getUserForEnrichment(freelancerId);
    }

    private FeignUserDTO getUserForEnrichment(Long userId) {
        try {
            log.info("Calling UserServiceClient.getUser with args={}", userId);
            FeignUserDTO user = userServiceClient.getUser(userId);
            log.info("UserServiceClient.getUser returned successfully");
            return user;
        } catch (FeignException.NotFound e) {
            throw new EntityNotFoundException("Freelancer not found with id: " + userId);
        } catch (FeignException e) {
            log.warn("Feign call to user-service failed: {}", e.getMessage());
            throw new IllegalStateException("User service temporarily unavailable");
        }
    }

    private FeignJobDTO getJobForEnrichment(Long jobId) {
        try {
            log.info("Calling JobServiceClient.getJob with args={}", jobId);
            FeignJobDTO job = jobServiceClient.getJob(jobId);
            log.info("JobServiceClient.getJob returned successfully");
            return job;
        } catch (FeignException.NotFound e) {
            throw new EntityNotFoundException("Job not found with id: " + jobId);
        } catch (FeignException e) {
            log.warn("Feign call to job-service failed: {}", e.getMessage());
            throw new IllegalStateException("Job service temporarily unavailable");
        }
    }

    private void assertProposalOwnerOrAdmin(Proposal proposal, HttpServletRequest request) {
        if (request == null) {
            return;
        }
        Long uid = proposalAuthSupport.extractUid(request);
        String role = proposalAuthSupport.extractRole(request);
        if (uid == null && role == null) {
            return;
        }
        if ("ADMIN".equalsIgnoreCase(role)) {
            return;
        }
        if (uid == null || !uid.equals(proposal.getFreelancerId())) {
            throw new ForbiddenOperationException("Not authorized to modify this proposal");
        }
    }

    private void logProposalTransition(Long proposalId, ProposalStatus oldStatus, ProposalStatus newStatus) {
        MDC.put("proposalId", String.valueOf(proposalId));
        try {
            log.info("Proposal {} transitioning {} -> {}", proposalId, oldStatus, newStatus);
        } finally {
            MDC.remove("proposalId");
        }
    }
}
