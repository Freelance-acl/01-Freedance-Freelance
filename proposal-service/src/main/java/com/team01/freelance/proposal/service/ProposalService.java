package com.team01.freelance.proposal.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.team01.freelance.common.observer.EventSubject;
import com.team01.freelance.contract.model.Contract;
import com.team01.freelance.contract.model.ContractStatus;
import com.team01.freelance.contract.repository.ContractRepository;
import com.team01.freelance.job.model.Job;
import com.team01.freelance.job.repository.JobRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.team01.freelance.proposal.cache.CacheKeyUtil;
import com.team01.freelance.proposal.cache.ProposalCacheInvalidationService;
import com.team01.freelance.proposal.cache.ProposalCacheService;
import com.team01.freelance.proposal.dto.FeeEstimateDTO;
import com.team01.freelance.proposal.dto.ProposalAnalyticsDTO;
import com.team01.freelance.proposal.dto.ProposalAnalyticsDashboardDTO;
import com.team01.freelance.proposal.dto.ProposalDetailsDTO;
import com.team01.freelance.proposal.dto.RecordInteractionResponse;
import com.team01.freelance.proposal.graph.InteractionGraphService;
import com.team01.freelance.proposal.model.MilestoneStatus;
import com.team01.freelance.proposal.model.Proposal;
import com.team01.freelance.proposal.model.ProposalMilestone;
import com.team01.freelance.proposal.model.ProposalStatus;
import com.team01.freelance.proposal.repository.ProposalAnalyticsProjection;
import com.team01.freelance.proposal.repository.ProposalDashboardProjection;
import com.team01.freelance.proposal.repository.ProposalRepository;
import com.team01.freelance.proposal.repository.ProposalStatusCountProjection;
import com.team01.freelance.user.model.User;
import com.team01.freelance.user.repository.UserRepository;
import com.team01.freelance.wallet.repository.PayoutRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class ProposalService {

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
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private PayoutRepository payoutRepository;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ProposalCacheService proposalCacheService;

    @Autowired
    private ProposalCacheInvalidationService cacheInvalidationService;

    @Autowired
    @Qualifier("proposalEventSubject")
    private EventSubject proposalEventSubject;

    @Autowired
    private InteractionGraphService interactionGraphService;

    public List<Proposal> getAllProposals() {
        return proposalRepository.findAll();
    }

    public Optional<Proposal> getProposalById(Long id) {
        String key = CacheKeyUtil.entityKey("proposal", id);
        Optional<Proposal> cached = proposalCacheService.get(key, Proposal.class);
        if (cached.isPresent()) {
            return cached;
        }
        Optional<Proposal> loaded = proposalRepository.findById(id);
        loaded.ifPresent(proposal -> proposalCacheService.put(key, Duration.ofMinutes(15), proposal));
        return loaded;
    }

    public ProposalDetailsDTO getProposalDetails(Long proposalId) {
        String key = CacheKeyUtil.featureKey("S3-F9", String.valueOf(proposalId));
        return proposalCacheService.getOrCompute(
                key,
                Duration.ofMinutes(10),
                ProposalDetailsDTO.class,
                () -> loadProposalDetails(proposalId));
    }

    private ProposalDetailsDTO loadProposalDetails(Long proposalId) {
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
        final ProposalStatus statusFilter =
                (status != null && !status.isBlank()) ? ProposalStatus.fromString(status.trim()) : null;
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime endExclusive = endDate.plusDays(1).atStartOfDay();
        Map<String, Object> params = Map.of(
                "status", status == null ? "" : status,
                "startDate", startDate.toString(),
                "endDate", endDate.toString());
        String key = CacheKeyUtil.featureKey("S3-F1", CacheKeyUtil.hashParams(params));
        return proposalCacheService.getOrCompute(
                key,
                Duration.ofMinutes(5),
                new TypeReference<List<Proposal>>() {
                },
                () -> proposalRepository.searchBySubmittedAtRangeAndOptionalStatus(start, endExclusive, statusFilter));
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
        Map<String, Object> params = Map.of("key", normalizedKey, "value", normalizedValue);
        String cacheKey = CacheKeyUtil.featureKey("S3-F5", CacheKeyUtil.hashParams(params));
        List<Proposal> matches = proposalCacheService.getOrCompute(
                cacheKey,
                Duration.ofMinutes(5),
                new TypeReference<List<Proposal>>() {
                },
                () -> findProposalsByMetadata(normalizedKey, normalizedValue));
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

        
    public ProposalAnalyticsDTO getProposalAnalytics(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        Map<String, Object> params = Map.of("startDate", startDate.toString(), "endDate", endDate.toString());
        String cacheKey = CacheKeyUtil.featureKey("S3-F6", CacheKeyUtil.hashParams(params));
        return proposalCacheService.getOrCompute(
                cacheKey,
                Duration.ofMinutes(10),
                ProposalAnalyticsDTO.class,
                () -> loadProposalAnalytics(startDate, endDate));
    }

    private ProposalAnalyticsDTO loadProposalAnalytics(LocalDate startDate, LocalDate endDate) {
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

    public ProposalAnalyticsDashboardDTO getProposalAnalyticsDashboard(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        logAnalyticsViewed();
        Map<String, Object> params = Map.of("startDate", startDate.toString(), "endDate", endDate.toString());
        String cacheKey = CacheKeyUtil.featureKey("S3-F10", CacheKeyUtil.hashParams(params));
        return proposalCacheService.getOrCompute(
                cacheKey,
                Duration.ofMinutes(10),
                ProposalAnalyticsDashboardDTO.class,
                () -> loadProposalAnalyticsDashboard(startDate, endDate));
    }

    private ProposalAnalyticsDashboardDTO loadProposalAnalyticsDashboard(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime endInclusive = endDate.atTime(23, 59, 59, 999_000_000);
        ProposalDashboardProjection stats =
                proposalRepository.calculateDashboardBySubmittedAtRange(start, endInclusive);
        long total = stats.getTotalProposals() != null ? stats.getTotalProposals() : 0L;
        long accepted = stats.getAcceptedProposals() != null ? stats.getAcceptedProposals() : 0L;
        double acceptanceRate = total == 0 ? 0.0 : (double) accepted / total;
        Map<String, Long> byStatus = proposalRepository.countProposalsByStatusInRange(start, endInclusive).stream()
                .collect(Collectors.toMap(
                        ProposalStatusCountProjection::getStatus,
                        ProposalStatusCountProjection::getCount,
                        Long::sum,
                        LinkedHashMap::new));
        return ProposalAnalyticsDashboardDTO.builder()
                .totalProposals(total)
                .acceptanceRate(acceptanceRate)
                .averageBidAmount(stats.getAverageBidAmount() != null ? stats.getAverageBidAmount() : 0.0)
                .averageEstimatedDays(stats.getAverageEstimatedDays() != null ? stats.getAverageEstimatedDays() : 0.0)
                .proposalsByStatus(byStatus)
                .build();
    }

    public FeeEstimateDTO estimatePlatformFee(Double bidAmount, Integer estimatedDays) {
        return estimatePlatformFee(bidAmount, estimatedDays, null);
    }

    public FeeEstimateDTO estimatePlatformFee(Double bidAmount, Integer estimatedDays, String requestBodyHash) {
        if (bidAmount == null || bidAmount <= 0) {
            throw new IllegalArgumentException("bidAmount must be positive");
        }
        if (estimatedDays == null || estimatedDays <= 0) {
            throw new IllegalArgumentException("estimatedDays must be positive");
        }
        String hash = requestBodyHash != null
                ? requestBodyHash
                : CacheKeyUtil.hashParams(Map.of("bidAmount", bidAmount, "estimatedDays", estimatedDays));
        String cacheKey = CacheKeyUtil.featureKey("S3-F3", hash);
        return proposalCacheService.getOrCompute(
                cacheKey,
                Duration.ofMinutes(5),
                FeeEstimateDTO.class,
                () -> computePlatformFee(bidAmount, estimatedDays));
    }

    public RecordInteractionResponse recordFreelancerJobInteraction(Long proposalId) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new EntityNotFoundException("Proposal not found with id: " + proposalId));
        if (proposal.getStatus() != ProposalStatus.SUBMITTED) {
            throw new IllegalArgumentException("Only SUBMITTED proposals can record interactions");
        }
        User freelancer = userRepository.findById(proposal.getFreelancerId())
                .orElseThrow(() -> new EntityNotFoundException("Freelancer not found with id: " + proposal.getFreelancerId()));
        Job job = jobRepository.findById(proposal.getJobId())
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + proposal.getJobId()));

        boolean mutated = interactionGraphService.recordInteraction(
                proposalId,
                proposal.getFreelancerId(),
                freelancer.getName(),
                proposal.getJobId(),
                job.getTitle(),
                job.getCategory() != null ? job.getCategory().name() : null);

        if (mutated) {
            Map<String, Object> details = Map.of(
                    "proposalId", proposalId,
                    "freelancerId", proposal.getFreelancerId(),
                    "jobId", proposal.getJobId());
            notifyProposalEvent(proposalId, "INTERACTION_RECORDED", details);
            cacheInvalidationService.invalidateRecommendations();
        }
        return new RecordInteractionResponse("Interaction recorded successfully");
    }

    public Proposal createProposal(Proposal proposal) {
        if (proposal.getFreelancerId() == null || proposal.getJobId() == null) {
            throw new IllegalArgumentException("Freelancer and Job IDs are required to create a Proposal");
        }

        jobRepository.findById(proposal.getJobId())
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + proposal.getJobId()));

        userRepository.findById(proposal.getFreelancerId())
                .orElseThrow(() -> new EntityNotFoundException("Freelancer not found with id: " + proposal.getFreelancerId()));

        Proposal saved = proposalRepository.save(proposal);
        cacheInvalidationService.invalidateAfterProposalWrite(saved.getId(), saved.getJobId());
        return saved;
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
            Proposal saved = proposalRepository.save(existingProposal);
            cacheInvalidationService.invalidateAfterProposalWrite(saved.getId(), saved.getJobId());
            return saved;
        }).orElseThrow(() -> new EntityNotFoundException("Proposal not found with id: " + id));
    }

    /**
     * Accepts a proposal, marks the job in progress, and creates an active contract transactionally.
     *
     * @param id the proposal ID
     * @return the accepted proposal
     * @throws EntityNotFoundException if the proposal, job, or freelancer user is not found
     * @throws IllegalArgumentException if the proposal status is not acceptable or the user is not a freelancer
     */
    @Transactional
    public Proposal acceptProposal(Long id) {
        Proposal proposal = proposalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Proposal not found with id: " + id));

        if (!ACCEPTABLE_STATUSES.contains(proposal.getStatus())) {
            throw new IllegalArgumentException("Only SUBMITTED or SHORTLISTED proposals can be accepted");
        }

        String freelancerRole = userRepository.findRoleByUserId(proposal.getFreelancerId());
        if (freelancerRole == null) {
            throw new EntityNotFoundException("Freelancer not found with id: " + proposal.getFreelancerId());
        }
        if (!"FREELANCER".equalsIgnoreCase(freelancerRole)) {
            throw new IllegalArgumentException("User is not a freelancer");
        }

        Job job = jobRepository.findById(proposal.getJobId())
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + proposal.getJobId()));

        LocalDateTime now = LocalDateTime.now();
        proposal.setStatus(ProposalStatus.ACCEPTED);
        proposal.setAcceptedAt(now);
        Proposal acceptedProposal = proposalRepository.save(proposal);

        jobRepository.markJobInProgress(proposal.getJobId());

        contractRepository.insertActiveContract(
                proposal.getJobId(),
                proposal.getFreelancerId(),
                job.getClientId(),
                proposal.getId(),
                proposal.getBidAmount(),
                now
        );

        acceptedProposal.setProposalMilestones(new ArrayList<>());
        notifyProposalEvent(
                acceptedProposal.getId(),
                "PROPOSAL_ACCEPTED",
                Map.of(
                        "proposalId", acceptedProposal.getId(),
                        "freelancerId", acceptedProposal.getFreelancerId(),
                        "jobId", acceptedProposal.getJobId()));
        cacheInvalidationService.invalidateAfterProposalWrite(acceptedProposal.getId(), acceptedProposal.getJobId());
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
        cacheInvalidationService.invalidateAfterProposalWrite(saved.getId(), saved.getJobId());
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
        Proposal proposal = proposalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Proposal not found with id: " + id));

        if (!WITHDRAWABLE_STATUSES.contains(proposal.getStatus())) {
            throw new IllegalArgumentException("Only SUBMITTED or SHORTLISTED proposals can be withdrawn");
        }

        long activeProposalCount = proposalRepository.countByJobIdAndStatusIn(
                proposal.getJobId(),
                WITHDRAWABLE_STATUSES);

        proposal.setStatus(ProposalStatus.WITHDRAWN);
        Proposal withdrawnProposal = proposalRepository.save(proposal);

        if (activeProposalCount == 1) {
            jobRepository.reopenIfInProgress(proposal.getJobId());
        }

        withdrawnProposal.setProposalMilestones(new ArrayList<>());
        cacheInvalidationService.invalidateAfterProposalWrite(withdrawnProposal.getId(), withdrawnProposal.getJobId());
        return withdrawnProposal;
    }

    /**
     * Completes work for an accepted proposal: closes the active contract, closes the job,
     * and creates a pending payout transactionally.
     *
     * @param id the proposal ID
     * @return the proposal (status remains ACCEPTED)
     * @throws EntityNotFoundException if the proposal is not found
     * @throws IllegalArgumentException if the proposal is not ACCEPTED or has no ACTIVE contract
     */
    @Transactional
    public Proposal completeProposal(Long id) {
        Proposal proposal = proposalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Proposal not found with id: " + id));

        if (proposal.getStatus() != ProposalStatus.ACCEPTED) {
            throw new IllegalArgumentException("Only ACCEPTED proposals can be completed");
        }

        Contract contract = contractRepository.findByProposalId(id)
                .filter(existing -> existing.getStatus() == ContractStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("No ACTIVE contract found for proposal"));

        LocalDateTime now = LocalDateTime.now();
        int contractsUpdated = contractRepository.completeActiveContract(contract.getId(), now);
        if (contractsUpdated != 1) {
            throw new IllegalStateException("Contract is no longer active or was already completed");
        }
        int jobsUpdated = jobRepository.markJobClosed(contract.getJobId());
        if (jobsUpdated != 1) {
            throw new IllegalStateException("Job could not be closed");
        }
        payoutRepository.insertPendingPayout(
                contract.getId(),
                contract.getFreelancerId(),
                contract.getAgreedAmount(),
                now
        );

        proposal.setProposalMilestones(new ArrayList<>());
        notifyProposalEvent(
                proposal.getId(),
                "PROPOSAL_COMPLETED",
                Map.of(
                        "proposalId", proposal.getId(),
                        "freelancerId", proposal.getFreelancerId(),
                        "jobId", contract.getJobId()));
        cacheInvalidationService.invalidateAfterProposalWrite(proposal.getId(), contract.getJobId());
        return proposal;
    }

    public boolean deleteProposalById(Long id) {
        if (!proposalRepository.existsById(id)) {
            return false;
        }
        proposalRepository.findById(id).ifPresent(proposal ->
                cacheInvalidationService.invalidateAfterProposalWrite(id, proposal.getJobId()));
        proposalRepository.deleteById(id);
        return true;
    }

    public void deleteAllProposals() {
        proposalRepository.deleteAll();
    }

    private FeeEstimateDTO computePlatformFee(double bidAmount, int estimatedDays) {
        double minBid = bidAmount * 0.8;
        double maxBid = bidAmount * 1.2;
        long similarProposalCount = proposalRepository.countActiveProposalsInSimilarBidRange(minBid, maxBid);
        int feePercentage = resolveFeePercentage(similarProposalCount);

        double platformFee = bidAmount * feePercentage / 100.0;
        double freelancerPayout = bidAmount - platformFee;
        double estimatedDailyRate = freelancerPayout / estimatedDays;

        return FeeEstimateDTO.builder()
                .bidAmount(bidAmount)
                .platformFee(platformFee)
                .freelancerPayout(freelancerPayout)
                .feePercentage(feePercentage)
                .estimatedDailyRate(estimatedDailyRate)
                .build();
    }

    private void logAnalyticsViewed() {
        notifyProposalEvent(null, "ANALYTICS_VIEWED", Map.of());
    }

    private void notifyProposalEvent(Long proposalId, String action, Map<String, Object> details) {
        Map<String, Object> payload = new HashMap<>();
        if (proposalId != null) {
            payload.put("proposalId", proposalId);
        } else {
            payload.put("proposalId", 0L);
        }
        payload.put("action", action);
        payload.put("details", details);
        proposalEventSubject.notifyObservers(action, payload);
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
}
