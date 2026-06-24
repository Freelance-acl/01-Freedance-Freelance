package com.team01.freelance.contract.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team01.freelance.contract.dto.ContractSummaryDTO;
import com.team01.freelance.contract.dto.ContractAnalyticsDTO;
import com.team01.freelance.contract.dto.BatchContractStatusUpdateRequest;
import com.team01.freelance.contract.dto.BatchContractStatusUpdateResponse;
import com.team01.freelance.contract.dto.ContractMilestoneDTO;
import com.team01.freelance.contract.dto.ContractMilestoneTrackRequest;
import com.team01.freelance.contract.dto.FreelancerPerformanceDTO;
import com.team01.freelance.contract.dto.StalledContractDTO;
import com.team01.freelance.contract.feign.JobServiceClient;
import com.team01.freelance.contract.feign.UserServiceClient;
import com.team01.freelance.contract.messaging.publishers.ContractEventPublisher;
import com.team01.freelance.contract.milestone.ContractMilestoneEvent;
import com.team01.freelance.contract.milestone.ContractMilestoneStatus;
import com.team01.freelance.contract.milestone.ContractMilestoneTimelineRepository;
import com.team01.freelance.contract.model.Contract;
import com.team01.freelance.contract.model.ContractStatus;
import com.team01.freelance.contract.observer.EntityObserver;
import com.team01.freelance.contract.repository.ContractRepository;
import com.team01.freelance.contracts.events.ProposalAcceptedEvent;
import com.team01.freelance.contracts.events.ProposalCancelledEvent;
import com.team01.freelance.contracts.events.ProposalCompletedEvent;
import com.team01.freelance.contracts.events.UserDeactivatedEvent;
import com.team01.freelance.contract.dto.JobDTO;
import com.team01.freelance.contract.dto.UserDTO;
import com.team01.freelance.contract.dto.UserContractSummaryDTO;
import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class ContractService {

    @Autowired
    private ContractRepository contractRepository;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @Autowired(required = false)
    private List<EntityObserver> observers = Collections.emptyList();

    @Autowired
    private ContractMilestoneTimelineRepository milestoneTimelineRepository;

    @Autowired
    private ContractAnalyticsCacheService contractAnalyticsCacheService;

    @Autowired(required = false)
    private UserServiceClient userServiceClient;

    @Autowired(required = false)
    private JobServiceClient jobServiceClient;

    @Autowired(required = false)
    private ContractEventPublisher contractEventPublisher;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Contract> getAllContracts() {
        return contractRepository.findAll();
    }

    public UserContractSummaryDTO getUserContractSummary(Long userId) {
        validateUserExists(userId, "User");
        long totalContracts = contractRepository.countContractsForUser(userId);
        long completedContracts = contractRepository.countCompletedContractsForUser(userId);
        long terminatedContracts = contractRepository.countTerminatedContractsForUser(userId);
        double totalEarnings = contractRepository.sumCompletedContractEarningsForUser(userId);
        double averageContractValue = totalContracts == 0 ? 0.0 : totalEarnings / totalContracts;
        return UserContractSummaryDTO.builder()
                .userId(userId)
                .name(resolveFreelancerName(userId))
                .totalContracts(totalContracts)
                .completedContracts(completedContracts)
                .terminatedContracts(terminatedContracts)
                .totalEarnings(totalEarnings)
                .averageContractValue(averageContractValue)
                .build();
    }

    public int getActiveContractCountForUser(Long userId) {
        validateUserExists(userId, "User");
        return contractRepository.countActiveContractsForUser(userId);
    }

    public long getCompletedContractCountForUser(Long userId) {
        validateUserExists(userId, "User");
        return contractRepository.countCompletedContractsForUser(userId);
    }

    public int getActiveContractCountForJob(Long jobId) {
        if (jobId == null) {
            throw new IllegalArgumentException("jobId is required");
        }
        return contractRepository.countActiveContractsForJob(jobId);
    }

    public Contract getActiveContractForProposal(Long proposalId) {
        if (proposalId == null) {
            throw new IllegalArgumentException("proposalId is required");
        }
        return contractRepository.findActiveContractByProposalId(proposalId)
                .orElseThrow(() -> new EntityNotFoundException("Active contract not found for proposal id: " + proposalId));
    }

    public ContractAnalyticsDTO getContractAnalytics(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if ((startDate == null) != (endDate == null)) {
            throw new IllegalArgumentException("Both startDate and endDate must be provided together, or neither");
        }
        if (startDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate must not be after endDate");
        }
        notifyObservers("ANALYTICS_VIEWED", Map.of(
                "contractId", -1L,
                "view", "contract-analytics"));
        return contractAnalyticsCacheService.getContractAnalytics(startDate, endDate);
    }

    public Optional<Contract> getContractById(Long id) {
        return contractRepository.findById(id);
    }

    public Contract getActiveContractForUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        validateUserExists(userId, "User");

        return contractRepository.findMostRecentActiveContractForUser(userId)
                .orElseThrow(() -> new EntityNotFoundException("No active contract found for user id: " + userId));
    }

    public Contract createContract(Contract contract) {
        if (contract.getFreelancerId() == null || contract.getJobId() == null ||
            contract.getClientId() == null || contract.getProposalId() == null) {
            throw new IllegalArgumentException("Freelancer, Job, Client, and Proposal IDs are required to create a Contract");
        }

        if (contract.getAgreedAmount() == null || contract.getAgreedAmount() <= 0) {
            throw new IllegalArgumentException("Agreed amount must be greater than 0");
        }

        if (contract.getStartDate() == null) {
            throw new IllegalArgumentException("Start date is required");
        }

        Contract created = contractRepository.save(contract);
        notifyObservers("CONTRACT_CREATED", Map.of(
                "contractId", created.getId(),
                "status", created.getStatus() == null ? "UNKNOWN" : created.getStatus().name()));
        return created;
    }

    /**
     * Updates editable fields on an existing contract.
     * Link fields (jobId, freelancerId, clientId, proposalId) are immutable after creation.
     *
     * @param id The ID of the contract to update
     * @param contractDetails The object containing updated fields
     * @return The updated contract
     * @throws EntityNotFoundException if the contract is not found
     */
    public Contract updateContract(Long id, Contract contractDetails) {
        return contractRepository.findById(id).map(existingContract -> {
                if (contractDetails.getAgreedAmount() != null) existingContract.setAgreedAmount(contractDetails.getAgreedAmount());
                if (contractDetails.getStatus() != null) existingContract.setStatus(contractDetails.getStatus());
                if (contractDetails.getStartDate() != null) existingContract.setStartDate(contractDetails.getStartDate());
                if (contractDetails.getEndDate() != null) existingContract.setEndDate(contractDetails.getEndDate());
                if (contractDetails.getMetadata() != null) existingContract.setMetadata(contractDetails.getMetadata());
                if (contractDetails.getCreatedAt() != null) existingContract.setCreatedAt(contractDetails.getCreatedAt());
            Contract updated = contractRepository.save(existingContract);
            notifyObservers("CONTRACT_UPDATED", Map.of(
                    "contractId", updated.getId(),
                    "status", updated.getStatus() == null ? "UNKNOWN" : updated.getStatus().name()));
            return updated;
        }).orElseThrow(() -> new EntityNotFoundException("Contract not found with id: " + id));
    }

    public Contract updateContractProgress(Long contractId, Map<String, Object> metadataUpdates) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new EntityNotFoundException("Contract not found with id: " + contractId));

        Map<String, Object> mergedMetadata = contract.getMetadata() == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(contract.getMetadata());
        if (metadataUpdates != null) {
            mergedMetadata.putAll(metadataUpdates);
        }

        contract.setMetadata(mergedMetadata);
        Contract updated = contractRepository.save(contract);
        Map<String, Object> eventPayload = new LinkedHashMap<>();
        eventPayload.put("contractId", contractId);
        eventPayload.put("progressPercentage", mergedMetadata.get("progressPercentage"));
        eventPayload.put("lastActivityDate", mergedMetadata.get("lastActivityDate"));
        notifyObservers("PROGRESS_UPDATED", eventPayload);
        return updated;
    }

    public List<ContractSummaryDTO> searchContracts(Double minAmount, Double maxAmount, String status) {
        if (minAmount == null || maxAmount == null) {
            throw new IllegalArgumentException("minAmount and maxAmount are required");
        }
        if (minAmount > maxAmount) {
            throw new IllegalArgumentException("minAmount must be less than or equal to maxAmount");
        }

        String normalizedStatus = null;
        if (status != null && !status.isBlank()) {
            normalizedStatus = ContractStatus.fromString(status).name();
        }

        Map<Long, String> freelancerNames = new LinkedHashMap<>();
        Map<Long, String> jobTitles = new LinkedHashMap<>();
        return contractRepository.searchContracts(minAmount, maxAmount, normalizedStatus).stream()
                .map(contract -> ContractSummaryDTO.builder()
                .contractId(contract.getId())
                .freelancerName(freelancerNames.computeIfAbsent(
                        contract.getFreelancerId(),
                        this::resolveFreelancerName))
                .jobTitle(jobTitles.computeIfAbsent(
                        contract.getJobId(),
                        this::resolveJobTitle))
                .agreedAmount(contract.getAgreedAmount())
                .status(contract.getStatus() == null ? null : contract.getStatus().name())
                .durationDays(calculateDurationDays(contract.getStartDate(), effectiveEndDate(contract)))
                .build()).toList();
    }

    public boolean deleteContractById(Long id) {
        Optional<Contract> contract = contractRepository.findById(id);
        if (contract.isEmpty()) {
            return false;
        }
        try {
            contractRepository.deleteById(id);
            notifyObservers("CONTRACT_DELETED", Map.of(
                    "contractId", id,
                    "previousStatus", contract.get().getStatus() == null ? "UNKNOWN" : contract.get().getStatus().name()));
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public void deleteAllContracts() {
        List<Long> ids = contractRepository.findAll().stream().map(Contract::getId).toList();
        contractRepository.deleteAll();
        for (Long id : ids) {
            notifyObservers("CONTRACT_DELETED", Map.of("contractId", id, "deletedVia", "DELETE_ALL"));
        }
    }

    @Transactional
    public long purgeOldContractData(int olderThanDays) {
        if (olderThanDays <= 0) {
            throw new IllegalArgumentException("olderThanDays must be greater than 0");
        }

        LocalDateTime cutoff = LocalDateTime.now().minusDays(olderThanDays);
        long deletedCount = contractRepository.countPurgeCandidates(cutoff);
        contractRepository.purgeOldContracts(cutoff);
        notifyObservers("OLD_DATA_PURGED", Map.of(
                "contractId", -1L,
                "olderThanDays", olderThanDays,
                "deletedCount", deletedCount));
        return deletedCount;
    }

    public FreelancerPerformanceDTO getFreelancerPerformanceSummary(
            Long freelancerId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("startDate and endDate are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must be on or after startDate");
        }
        validateUserExists(freelancerId, "Freelancer");

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime endExclusive = endDate.plusDays(1).atStartOfDay();
        if (!usesPostgresDatabase()) {
            return buildFreelancerPerformanceInMemory(freelancerId, start, endExclusive);
        }
        try {
            Object[] raw = contractRepository.getFreelancerPerformance(freelancerId, start, endExclusive);
            long totalContracts = toLong(raw[0]);
            long completedContracts = toLong(raw[1]);
            double totalEarnings = toDouble(raw[2]);
            double averageContractValue = toDouble(raw[3]);
            double averageDurationDays = toDouble(raw[4]);
            double completionRate = totalContracts == 0 ? 0.0 : (completedContracts * 100.0) / totalContracts;

            return FreelancerPerformanceDTO.builder()
                    .freelancerId(freelancerId)
                    .totalContracts(totalContracts)
                    .averageContractValue(averageContractValue)
                    .completionRate(completionRate)
                    .averageDurationDays(averageDurationDays)
                    .totalEarnings(totalEarnings)
                    .build();
        } catch (DataAccessException ex) {
            return buildFreelancerPerformanceInMemory(freelancerId, start, endExclusive);
        }
    }

    public List<StalledContractDTO> findStalledContracts(Double maxProgress, Integer stalledDays) {
        if (maxProgress == null || stalledDays == null) {
            throw new IllegalArgumentException("maxProgress and stalledDays are required");
        }
        if (maxProgress < 0 || maxProgress > 100) {
            throw new IllegalArgumentException("maxProgress must be between 0 and 100");
        }
        if (stalledDays < 0) {
            throw new IllegalArgumentException("stalledDays must be 0 or greater");
        }

        if (!usesPostgresDatabase()) {
            return findStalledContractsInMemory(maxProgress, stalledDays);
        }
        try {
            return enrichStalledContracts(contractRepository.findStalledContracts(maxProgress, stalledDays));
        } catch (DataAccessException ex) {
            return findStalledContractsInMemory(maxProgress, stalledDays);
        }
    }

    public List<Contract> findContractHistory(LocalDate startDate, LocalDate endDate, String status) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("startDate and endDate are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must be on or after startDate");
        }

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime endExclusive = endDate.plusDays(1).atStartOfDay();
        if (status == null || status.isBlank()) {
            return contractRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAsc(
                    start,
                    endExclusive
            );
        }
        return contractRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanAndStatusOrderByCreatedAtAsc(
                start,
                endExclusive,
                ContractStatus.fromString(status)
        );
    }

    public List<Contract> findContractsByMetadata(String key, String operator, String value) {
        if (key == null || key.isBlank() || value == null || value.isBlank()) {
            throw new IllegalArgumentException("key and value are required");
        }
        if (operator == null || operator.isBlank()) {
            throw new IllegalArgumentException("operator is required");
        }

        String normalizedKey = key.trim();
        String normalizedValue = value.trim();
        String normalizedOperator = operator.trim().toLowerCase();

        try {
            return switch (normalizedOperator) {
                case "eq" -> contractRepository.findByMetadataEquals(normalizedKey, normalizedValue);
                case "gt" -> contractRepository.findByMetadataGreaterThan(normalizedKey, parseMetadataNumber(normalizedValue));
                case "lt" -> contractRepository.findByMetadataLessThan(normalizedKey, parseMetadataNumber(normalizedValue));
                default -> throw new IllegalArgumentException("operator must be one of: eq, gt, lt");
            };
        } catch (DataAccessException e) {
            return filterMetadataFromJdbc(normalizedKey, normalizedOperator, normalizedValue);
        }
    }

    private List<Contract> filterMetadataFromJdbc(String key, String operator, String value) {
        List<Contract> contracts = jdbcTemplate == null
                ? contractRepository.findAll()
                : jdbcTemplate.query("""
                        SELECT id, job_id, freelancer_id, client_id, proposal_id, agreed_amount,
                               status, start_date, end_date, metadata, created_at
                        FROM contracts
                        ORDER BY id ASC
                        """, (rs, rowNum) -> {
                    Contract contract = new Contract();
                    contract.setId(rs.getLong("id"));
                    contract.setJobId(rs.getLong("job_id"));
                    contract.setFreelancerId(rs.getLong("freelancer_id"));
                    contract.setClientId(rs.getLong("client_id"));
                    contract.setProposalId(rs.getLong("proposal_id"));
                    contract.setAgreedAmount(rs.getDouble("agreed_amount"));
                    contract.setStatus(ContractStatus.fromString(rs.getString("status")));
                    contract.setStartDate(rs.getTimestamp("start_date").toLocalDateTime());
                    if (rs.getTimestamp("end_date") != null) {
                        contract.setEndDate(rs.getTimestamp("end_date").toLocalDateTime());
                    }
                    contract.setMetadata(parseMetadata(rs.getObject("metadata")));
                    contract.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    return contract;
                });

        return contracts.stream()
                .filter(contract -> contract.getMetadata() != null && contract.getMetadata().containsKey(key))
                .filter(contract -> metadataMatches(contract.getMetadata().get(key), operator, value))
                .toList();
    }

    private Map<String, Object> parseMetadata(Object metadata) {
        if (metadata == null) {
            return null;
        }
        String json = metadata instanceof byte[] bytes
                ? new String(bytes, java.nio.charset.StandardCharsets.UTF_8)
                : metadata.toString();
        if (json.isBlank()) {
            return null;
        }
        try {
            if (json.startsWith("\"") && json.endsWith("\"")) {
                json = objectMapper.readValue(json, String.class);
            }
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid contract metadata JSON", e);
        }
    }

    private boolean metadataMatches(Object actualValue, String operator, String expectedValue) {
        if (actualValue == null) {
            return false;
        }

        return switch (operator) {
            case "eq" -> expectedValue.equals(String.valueOf(actualValue));
            case "gt" -> toMetadataDouble(actualValue) > parseMetadataNumber(expectedValue);
            case "lt" -> toMetadataDouble(actualValue) < parseMetadataNumber(expectedValue);
            default -> throw new IllegalArgumentException("operator must be one of: eq, gt, lt");
        };
    }

    private double toMetadataDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return parseMetadataNumber(String.valueOf(value));
    }

    private Double parseMetadataNumber(String value) {
        try {
            return Double.valueOf(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("value must be numeric for gt and lt operators");
        }
    }

    @Transactional
    public BatchContractStatusUpdateResponse batchUpdateContractStatus(List<BatchContractStatusUpdateRequest> updates) {
        if (updates == null || updates.isEmpty()) {
            throw new IllegalArgumentException("At least one contract status update is required");
        }

        List<Long> contractIds = updates.stream()
                .map(BatchContractStatusUpdateRequest::getContractId)
                .toList();
        if (contractIds.stream().anyMatch(id -> id == null)) {
            throw new IllegalArgumentException("contractId is required for every update");
        }
        if (updates.stream().anyMatch(update -> update.getStatus() == null)) {
            throw new IllegalArgumentException("status is required for every update");
        }

        List<Contract> contracts = contractRepository.findAllById(contractIds);
        if (contracts.size() != Set.copyOf(contractIds).size()) {
            throw new EntityNotFoundException("One or more contracts were not found");
        }

        Map<Long, Contract> contractsById = contracts.stream()
                .collect(java.util.stream.Collectors.toMap(Contract::getId, contract -> contract));
        for (BatchContractStatusUpdateRequest update : updates) {
            Contract contract = contractsById.get(update.getContractId());
            ContractStatus newStatus = update.getStatus();
            validateStatusTransition(contract.getStatus(), newStatus);
            contract.setStatus(newStatus);
            if (newStatus == ContractStatus.COMPLETED && contract.getEndDate() == null) {
                contract.setEndDate(LocalDateTime.now());
            }
        }

        contractRepository.saveAllAndFlush(contracts);
        return new BatchContractStatusUpdateResponse(contracts.size());
    }

    private void validateStatusTransition(ContractStatus currentStatus, ContractStatus newStatus) {
        if (currentStatus == newStatus) {
            return;
        }
        if (currentStatus == ContractStatus.COMPLETED || currentStatus == ContractStatus.TERMINATED) {
            throw new IllegalArgumentException("Completed or terminated contracts cannot be moved to another status");
        }
    }

    private FreelancerPerformanceDTO buildFreelancerPerformanceInMemory(
            Long freelancerId,
            LocalDateTime start,
            LocalDateTime endExclusive
    ) {
        List<Contract> inRange = contractRepository.findAll().stream()
                .filter(contract -> freelancerId.equals(contract.getFreelancerId()))
                .filter(contract -> contract.getCreatedAt() != null
                        && !contract.getCreatedAt().isBefore(start)
                        && contract.getCreatedAt().isBefore(endExclusive))
                .toList();

        long totalContracts = inRange.size();
        List<Contract> completed = inRange.stream()
                .filter(contract -> contract.getStatus() == ContractStatus.COMPLETED)
                .toList();
        long completedContracts = completed.size();
        double totalEarnings = completed.stream()
                .mapToDouble(Contract::getAgreedAmount)
                .sum();
        double averageContractValue = totalContracts == 0 ? 0.0 : totalEarnings / totalContracts;
        double averageDurationDays = completed.stream()
                .mapToLong(contract -> calculateDurationDays(contract.getStartDate(), contract.getEndDate()))
                .average()
                .orElse(0.0);
        double completionRate = totalContracts == 0 ? 0.0 : (completedContracts * 100.0) / totalContracts;

        return FreelancerPerformanceDTO.builder()
                .freelancerId(freelancerId)
                .totalContracts(totalContracts)
                .averageContractValue(averageContractValue)
                .completionRate(completionRate)
                .averageDurationDays(averageDurationDays)
                .totalEarnings(totalEarnings)
                .build();
    }

    private List<StalledContractDTO> findStalledContractsInMemory(Double maxProgress, Integer stalledDays) {
        LocalDateTime activityCutoff = LocalDateTime.now().minusDays(stalledDays);
        List<Contract> candidates = contractRepository.findByStatus(ContractStatus.ACTIVE).stream()
                .filter(contract -> readMetadataDouble(contract, "progressPercentage") <= maxProgress)
                .filter(contract -> {
                    LocalDateTime lastActivity = resolveLastActivity(contract);
                    return lastActivity != null && lastActivity.isBefore(activityCutoff);
                })
                .toList();
        return enrichStalledContracts(candidates);
    }

    @Transactional
    @CacheEvict(value = "contractMilestoneTimeline", allEntries = true)
    public void recordContractMilestone(Long contractId, ContractMilestoneTrackRequest request) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new EntityNotFoundException("Contract not found with id: " + contractId));

        if (request.getMilestoneOrder() == null || request.getMilestoneOrder() <= 0) {
            throw new IllegalArgumentException("milestoneOrder must be greater than 0");
        }
        ContractMilestoneStatus status = ContractMilestoneStatus.fromString(request.getStatus());
        ContractMilestoneEvent event = new ContractMilestoneEvent(
                contractId,
                LocalDateTime.now(),
                request.getMilestoneOrder(),
                status,
                request.getRecordedBy(),
                request.getNotes()
        );
        milestoneTimelineRepository.save(event);
        notifyObservers("MILESTONE_TRACKED", Map.of(
                "contractId", contract.getId(),
                "milestoneOrder", event.milestoneOrder(),
                "status", event.status().name(),
                "recordedBy", event.recordedBy() == null ? "" : event.recordedBy()));
    }

    @Cacheable(value = "contractMilestoneTimeline", key = "#contractId + '|' + (#startTime == null ? 'null' : #startTime.toString()) + '|' + (#endTime == null ? 'null' : #endTime.toString())")
    public List<ContractMilestoneDTO> getContractMilestoneTimeline(Long contractId, LocalDateTime startTime, LocalDateTime endTime) {
        contractRepository.findById(contractId)
                .orElseThrow(() -> new EntityNotFoundException("Contract not found with id: " + contractId));
        return milestoneTimelineRepository.findByContractId(contractId, startTime, endTime).stream()
                .map(event -> ContractMilestoneDTO.builder()
                        .timestamp(event.timestamp())
                        .milestoneOrder(event.milestoneOrder())
                        .status(event.status().name())
                        .recordedBy(event.recordedBy())
                        .notes(event.notes())
                        .build())
                .toList();
    }

    @Transactional
    public Contract handleProposalAccepted(ProposalAcceptedEvent event) {
        if (event == null || event.proposalId() == null || event.jobId() == null || event.freelancerId() == null) {
            throw new IllegalArgumentException("proposal.accepted event is missing required fields");
        }
        Optional<Contract> existing = contractRepository.findByProposalId(event.proposalId());
        if (existing.isPresent()) {
            return existing.get();
        }

        JobDTO job = fetchJob(event.jobId());
        Contract contract = new Contract();
        contract.setProposalId(event.proposalId());
        contract.setJobId(event.jobId());
        contract.setFreelancerId(event.freelancerId());
        contract.setClientId(job.getClientId());
        contract.setAgreedAmount(toDouble(event.bidAmount()));
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setStartDate(LocalDateTime.now());
        Contract created = contractRepository.saveAndFlush(contract);
        publishContractCreated(created);
        return created;
    }

    @Transactional
    public Contract handleProposalCompleted(ProposalCompletedEvent event) {
        if (event == null || event.proposalId() == null) {
            throw new IllegalArgumentException("proposal.completed event is missing proposalId");
        }
        Contract contract = contractRepository.findActiveContractByProposalId(event.proposalId())
                .or(() -> event.contractId() == null ? Optional.empty() : contractRepository.findById(event.contractId()))
                .orElseThrow(() -> new EntityNotFoundException("Active contract not found for proposal id: " + event.proposalId()));
        ContractStatus oldStatus = contract.getStatus();
        contract.setStatus(ContractStatus.COMPLETED);
        contract.setEndDate(LocalDateTime.now());
        Contract updated = contractRepository.saveAndFlush(contract);
        publishContractStatusChanged(updated.getId(), oldStatus, updated.getStatus());
        return updated;
    }

    @Transactional
    public Optional<Contract> handleProposalCancelled(ProposalCancelledEvent event) {
        if (event == null || event.proposalId() == null) {
            throw new IllegalArgumentException("proposal.cancelled event is missing proposalId");
        }
        Optional<Contract> existing = contractRepository.findByProposalId(event.proposalId());
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        Contract contract = existing.get();
        contract.setStatus(ContractStatus.TERMINATED);
        if (contract.getEndDate() == null) {
            contract.setEndDate(LocalDateTime.now());
        }
        Contract updated = contractRepository.saveAndFlush(contract);
        if (contractEventPublisher != null) {
            contractEventPublisher.publishContractCancelled(updated);
        }
        return Optional.of(updated);
    }

    public void handleUserDeactivated(UserDeactivatedEvent event) {
        if (event == null || event.userId() == null) {
            throw new IllegalArgumentException("user.deactivated event is missing userId");
        }
        notifyObservers("USER_DEACTIVATED", Map.of("contractId", -1L, "userId", event.userId()));
    }

    private List<StalledContractDTO> enrichStalledContracts(List<Contract> contracts) {
        Map<Long, String> freelancerNames = new LinkedHashMap<>();
        Map<Long, String> jobTitles = new LinkedHashMap<>();
        return contracts.stream()
                .map(contract -> {
                    LocalDateTime lastActivity = resolveLastActivity(contract);
                    long daysSince = lastActivity == null
                            ? 0L
                            : ChronoUnit.DAYS.between(lastActivity, LocalDateTime.now());
                    return StalledContractDTO.builder()
                            .contractId(contract.getId())
                            .freelancerName(freelancerNames.computeIfAbsent(
                                    contract.getFreelancerId(),
                                    this::resolveFreelancerName))
                            .jobTitle(jobTitles.computeIfAbsent(
                                    contract.getJobId(),
                                    this::resolveJobTitle))
                            .agreedAmount(contract.getAgreedAmount())
                            .progressPercentage(readMetadataDouble(contract, "progressPercentage"))
                            .daysSinceLastActivity(daysSince)
                            .build();
                })
                .toList();
    }

    private void validateUserExists(Long userId, String label) {
        if (userId == null) {
            throw new IllegalArgumentException(label + " id is required");
        }
        if (userServiceClient == null || !usesPostgresDatabase()) {
            return;
        }
        try {
            userServiceClient.getUser(userId);
        } catch (FeignException.NotFound e) {
            throw new EntityNotFoundException(label + " not found with id: " + userId);
        } catch (Exception e) {
            Boolean existsLocally = localUserExists(userId);
            if (Boolean.FALSE.equals(existsLocally)) {
                throw new EntityNotFoundException(label + " not found with id: " + userId);
            }
        }
    }

    private String resolveFreelancerName(Long freelancerId) {
        if (freelancerId == null) {
            return "Unknown Freelancer";
        }
        if (userServiceClient != null && usesPostgresDatabase()) {
            try {
                UserDTO user = userServiceClient.getUser(freelancerId);
                return user == null || user.getName() == null ? "Unknown Freelancer" : user.getName();
            } catch (FeignException.NotFound e) {
                return "Unknown Freelancer";
            } catch (Exception e) {
                String fallbackName = queryLocalUserName(freelancerId);
                return fallbackName == null ? "Unknown Freelancer" : fallbackName;
            }
        }
        String fallbackName = queryLocalUserName(freelancerId);
        return fallbackName == null ? "Unknown Freelancer" : fallbackName;
    }

    private String resolveJobTitle(Long jobId) {
        if (jobId == null) {
            return "Unknown Job";
        }
        if (jobServiceClient != null && usesPostgresDatabase()) {
            try {
                JobDTO job = jobServiceClient.getJob(jobId);
                return job == null || job.getTitle() == null ? "Unknown Job" : job.getTitle();
            } catch (FeignException.NotFound e) {
                return "Unknown Job";
            } catch (Exception e) {
                String fallbackTitle = queryLocalJobTitle(jobId);
                return fallbackTitle == null ? "Unknown Job" : fallbackTitle;
            }
        }
        String fallbackTitle = queryLocalJobTitle(jobId);
        return fallbackTitle == null ? "Unknown Job" : fallbackTitle;
    }

    private Boolean localUserExists(Long userId) {
        if (jdbcTemplate != null) {
            try {
                Integer count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM users WHERE id = ?", Integer.class, userId);
                return count != null && count > 0;
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private String queryLocalUserName(Long userId) {
        if (jdbcTemplate == null) {
            return null;
        }
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT name FROM users WHERE id = ?", String.class, userId);
        } catch (Exception e) {
            return null;
        }
    }

    private String queryLocalJobTitle(Long jobId) {
        if (jdbcTemplate == null) {
            return null;
        }
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT title FROM jobs WHERE id = ?", String.class, jobId);
        } catch (Exception e) {
            return null;
        }
    }

    private JobDTO fetchJob(Long jobId) {
        if (jobServiceClient == null) {
            throw new IllegalStateException("job-service client is not configured");
        }
        try {
            return jobServiceClient.getJob(jobId);
        } catch (FeignException.NotFound e) {
            throw new EntityNotFoundException("Job not found with id: " + jobId);
        }
    }

    private LocalDateTime effectiveEndDate(Contract contract) {
        return contract.getEndDate() == null ? LocalDateTime.now() : contract.getEndDate();
    }

    private void publishContractCreated(Contract contract) {
        if (contractEventPublisher != null) {
            contractEventPublisher.publishContractCreated(contract);
        }
    }

    private void publishContractStatusChanged(Long contractId, ContractStatus oldStatus, ContractStatus newStatus) {
        if (contractEventPublisher != null) {
            contractEventPublisher.publishContractStatusChanged(contractId, oldStatus, newStatus);
        }
    }

    private LocalDateTime resolveLastActivity(Contract contract) {
        if (contract.getMetadata() != null && contract.getMetadata().get("lastActivityDate") != null) {
            LocalDateTime parsed = toLocalDateTime(contract.getMetadata().get("lastActivityDate"));
            if (parsed != null) {
                return parsed;
            }
        }
        return contract.getCreatedAt();
    }

    private double readMetadataDouble(Contract contract, String key) {
        if (contract.getMetadata() == null || contract.getMetadata().get(key) == null) {
            return 0.0;
        }
        Object value = contract.getMetadata().get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }

    private boolean usesPostgresDatabase() {
        if (dataSource == null) {
            return false;
        }
        try (var connection = dataSource.getConnection()) {
            return "PostgreSQL".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName());
        } catch (Exception ex) {
            return false;
        }
    }

    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        return ((Number) value).longValue();
    }

    private double toDouble(Object value) {
        if (value == null) {
            return 0.0;
        }
        return ((Number) value).doubleValue();
    }

    private double toDouble(BigDecimal value) {
        return value == null ? 0.0 : value.doubleValue();
    }

    private long calculateDurationDays(Object startValue, Object endValue) {
        LocalDateTime start = toLocalDateTime(startValue);
        LocalDateTime end = toLocalDateTime(endValue);
        if (start == null || end == null) {
            return 0L;
        }
        return Math.max(0L, ChronoUnit.DAYS.between(start, end));
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toLocalDateTime();
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (value instanceof String stringValue) {
            return LocalDateTime.parse(stringValue);
        }
        throw new IllegalArgumentException("Unsupported date value: " + value.getClass().getName());
    }

    private void notifyObservers(String eventType, Map<String, Object> payload) {
        for (EntityObserver observer : observers) {
            observer.onEvent(eventType, payload);
        }
    }
}
