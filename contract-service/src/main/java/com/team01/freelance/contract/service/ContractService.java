package com.team01.freelance.contract.service;

import com.team01.freelance.contract.dto.FreelancerPerformanceDTO;
import com.team01.freelance.contract.dto.StalledContractDTO;
import com.team01.freelance.contract.model.Contract;
import com.team01.freelance.contract.repository.ContractRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ContractService {

    @Autowired
    private ContractRepository contractRepository;

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
}
