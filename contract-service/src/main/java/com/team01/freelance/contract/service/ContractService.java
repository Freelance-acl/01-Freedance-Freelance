package com.team01.freelance.contract.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team01.freelance.contract.dto.ContractSummaryDTO;
import com.team01.freelance.contract.dto.BatchContractStatusUpdateRequest;
import com.team01.freelance.contract.dto.BatchContractStatusUpdateResponse;
import com.team01.freelance.contract.dto.FreelancerPerformanceDTO;
import com.team01.freelance.contract.dto.StalledContractDTO;
import com.team01.freelance.contract.model.Contract;
import com.team01.freelance.contract.model.ContractStatus;
import com.team01.freelance.contract.repository.ContractRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
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

    private final ObjectMapper objectMapper = new ObjectMapper();

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

        return contractRepository.save(contract);
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
            return contractRepository.save(existingContract);
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
        return contractRepository.save(contract);
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
        return rows.stream().map(row -> new ContractSummaryDTO(
                toLong(row[0]),
                row[1] == null ? null : row[1].toString(),
                row[2] == null ? null : row[2].toString(),
                toDouble(row[3]),
                row[4] == null ? null : row[4].toString(),
                calculateDurationDays(row[5], row[6])
        )).toList();
    }

    public boolean deleteContractById(Long id) {
        try {
            contractRepository.deleteById(id);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public void deleteAllContracts() {
        contractRepository.deleteAll();
    }

    @Transactional
    public long purgeOldContractData(int olderThanDays) {
        if (olderThanDays <= 0) {
            throw new IllegalArgumentException("olderThanDays must be greater than 0");
        }

        LocalDateTime cutoff = LocalDateTime.now().minusDays(olderThanDays);
        long deletedCount = contractRepository.countPurgeCandidates(cutoff);
        contractRepository.purgeOldContracts(cutoff);
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

        return new FreelancerPerformanceDTO(
                freelancerId,
                totalContracts,
                averageContractValue,
                completionRate,
                averageDurationDays,
                totalEarnings
        );
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
        return rows.stream().map(row -> new StalledContractDTO(
                toLong(row[0]),
                row[1] == null ? null : row[1].toString(),
                row[2] == null ? null : row[2].toString(),
                toDouble(row[3]),
                toDouble(row[4]),
                toLong(row[5])
        )).toList();
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
