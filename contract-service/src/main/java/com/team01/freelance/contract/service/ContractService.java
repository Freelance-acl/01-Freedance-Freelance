package com.team01.freelance.contract.service;

import com.team01.freelance.contract.cache.RedisCacheService;
import com.team01.freelance.contract.dto.ContractAnalyticsDTO;
import com.team01.freelance.contract.dto.ContractSummaryDTO;
import com.team01.freelance.contract.dto.FreelancerPerformanceDTO;
import com.team01.freelance.contract.dto.StalledContractDTO;
import com.team01.freelance.contract.event.EntityObserver;
import com.team01.freelance.contract.event.MongoEventLogger;
import com.team01.freelance.contract.model.Contract;
import com.team01.freelance.contract.model.ContractStatus;
import com.team01.freelance.contract.repository.ContractRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.annotation.PostConstruct;

@Service
public class ContractService {

    @Autowired
    private ContractRepository contractRepository;

    @Autowired(required = false)
    private RedisCacheService redisCacheService;

    @Autowired(required = false)
    private MongoEventLogger mongoEventLogger;

    private final List<EntityObserver> observers = new CopyOnWriteArrayList<>();

    @PostConstruct
    void registerDefaultObservers() {
        register(mongoEventLogger);
    }

    public void register(EntityObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public void unregister(EntityObserver observer) {
        observers.remove(observer);
    }

    public List<Contract> getAllContracts() {
        return contractRepository.findAll();
    }

    public Optional<Contract> getContractById(Long id) {
        return contractRepository.findById(id);
    }

    public Contract getActiveContractForUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (!contractRepository.userExists(userId)) {
            throw new EntityNotFoundException("User not found with id: " + userId);
        }

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

        Contract savedContract = contractRepository.save(contract);
        evictAnalyticsCache();
        return savedContract;
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
            Contract savedContract = contractRepository.save(existingContract);
            evictAnalyticsCache();
            return savedContract;
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
        Contract updatedContract = contractRepository.save(contract);
        notifyObservers("PROGRESS_UPDATED", Map.of(
                "contractId", contractId,
                "details", new LinkedHashMap<>(mergedMetadata)
        ));
        evictAnalyticsCache();
        return updatedContract;
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

        List<Object[]> rows = contractRepository.searchContracts(minAmount, maxAmount, normalizedStatus);
        return rows.stream().map(row -> ContractSummaryDTO.builder()
                .contractId(toLong(row[0]))
                .freelancerName(row[1] == null ? null : row[1].toString())
                .jobTitle(row[2] == null ? null : row[2].toString())
                .agreedAmount(toDouble(row[3]))
                .status(row[4] == null ? null : row[4].toString())
                .durationDays(calculateDurationDays(row[5], row[6]))
                .build()
        ).toList();
    }

    public ContractAnalyticsDTO getContractAnalytics(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("startDate and endDate are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must be on or after startDate");
        }

        notifyObservers("ANALYTICS_VIEWED", Map.of(
                "contractId", 0L,
                "details", Map.of(
                        "startDate", startDate.toString(),
                        "endDate", endDate.toString()
                )
        ));

        String cacheKey = "contract-service::S4-F10::" + startDate + "::" + endDate;
        if (redisCacheService == null) {
            return computeContractAnalytics(startDate, endDate);
        }
        return redisCacheService.getOrCompute(
                cacheKey,
                ContractAnalyticsDTO.class,
                Duration.ofMinutes(10),
                () -> computeContractAnalytics(startDate, endDate)
        );
    }

    public boolean deleteContractById(Long id) {
        try {
            contractRepository.deleteById(id);
            evictAnalyticsCache();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public void deleteAllContracts() {
        contractRepository.deleteAll();
        evictAnalyticsCache();
    }

    @Transactional
    public long purgeOldContractData(int olderThanDays) {
        if (olderThanDays <= 0) {
            throw new IllegalArgumentException("olderThanDays must be greater than 0");
        }

        LocalDateTime cutoff = LocalDateTime.now().minusDays(olderThanDays);
        long deletedCount = contractRepository.countPurgeCandidates(cutoff);
        contractRepository.purgeOldContracts(cutoff);
        if (deletedCount > 0) {
            evictAnalyticsCache();
        }
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
        if (!contractRepository.freelancerExists(freelancerId)) {
            throw new EntityNotFoundException("Freelancer not found with id: " + freelancerId);
        }

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime endExclusive = endDate.plusDays(1).atStartOfDay();
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

        List<Object[]> rows = contractRepository.findStalledContracts(maxProgress, stalledDays);
        return rows.stream().map(row -> StalledContractDTO.builder()
                .contractId(toLong(row[0]))
                .freelancerName(row[1] == null ? null : row[1].toString())
                .jobTitle(row[2] == null ? null : row[2].toString())
                .agreedAmount(toDouble(row[3]))
                .progressPercentage(toDouble(row[4]))
                .daysSinceLastActivity(toLong(row[5]))
                .build()
        ).toList();
    }

    private ContractAnalyticsDTO computeContractAnalytics(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime endExclusive = endDate.plusDays(1).atStartOfDay();

        Object[] summary = contractRepository.getContractAnalyticsSummary(start, endExclusive)
                .stream()
                .findFirst()
                .orElseGet(() -> new Object[]{0L, 0.0, 0L, 0.0});
        long totalContracts = toLong(summary[0]);
        double averageContractValue = toDouble(summary[1]);
        long completedContracts = toLong(summary[2]);
        double averageContractDurationDays = toDouble(summary[3]);
        double completionRate = totalContracts == 0 ? 0.0 : ((double) completedContracts) / totalContracts;

        Map<String, Long> contractsByStatus = new LinkedHashMap<>();
        for (Object[] row : contractRepository.getContractCountsByStatus(start, endExclusive)) {
            contractsByStatus.put(row[0].toString(), toLong(row[1]));
        }

        return ContractAnalyticsDTO.builder()
                .totalContracts(totalContracts)
                .averageContractValue(averageContractValue)
                .completionRate(completionRate)
                .averageContractDurationDays(averageContractDurationDays)
                .contractsByStatus(contractsByStatus)
                .build();
    }

    private void notifyObservers(String action, Map<String, Object> payload) {
        for (EntityObserver observer : observers) {
            observer.onEvent(action, payload);
        }
    }

    private void evictAnalyticsCache() {
        if (redisCacheService != null) {
            redisCacheService.evictByPrefix("contract-service::S4-F10::");
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
        throw new IllegalArgumentException("Unsupported date value: " + value.getClass().getName());
    }
}
