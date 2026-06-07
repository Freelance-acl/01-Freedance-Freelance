package com.team01.freelance.contract.service;

import com.team01.freelance.contract.dto.ContractAnalyticsDTO;
import com.team01.freelance.contract.model.Contract;
import com.team01.freelance.contract.model.ContractStatus;
import com.team01.freelance.contract.repository.ContractRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ContractAnalyticsCacheService {

    private final ContractRepository contractRepository;
    private final DataSource dataSource;

    public ContractAnalyticsCacheService(ContractRepository contractRepository, DataSource dataSource) {
        this.contractRepository = contractRepository;
        this.dataSource = dataSource;
    }

    @Cacheable(value = "S4-F10", key = "'analytics'")
    public ContractAnalyticsDTO getContractAnalytics() {
        if (!usesPostgresDatabase()) {
            return buildAnalyticsInMemory();
        }
        try {
            Object[] row = contractRepository.getContractAnalytics();
            Map<String, Long> byStatus = new LinkedHashMap<>();
            for (Object[] statusRow : contractRepository.countContractsByStatus()) {
                byStatus.put(String.valueOf(statusRow[0]), ((Number) statusRow[1]).longValue());
            }
            return ContractAnalyticsDTO.builder()
                    .totalContracts(((Number) row[0]).longValue())
                    .avgValue(((Number) row[1]).doubleValue())
                    .completionRate(((Number) row[2]).doubleValue())
                    .avgDurationDays(((Number) row[3]).doubleValue())
                    .byStatus(byStatus)
                    .build();
        } catch (DataAccessException ex) {
            return buildAnalyticsInMemory();
        }
    }

    private ContractAnalyticsDTO buildAnalyticsInMemory() {
        List<Contract> contracts = contractRepository.findAll();
        long totalContracts = contracts.size();
        double avgValue = contracts.stream()
                .map(Contract::getAgreedAmount)
                .filter(amount -> amount != null)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
        long completedContracts = contracts.stream()
                .filter(contract -> contract.getStatus() == ContractStatus.COMPLETED)
                .count();
        double completionRate = totalContracts == 0 ? 0.0 : (completedContracts * 100.0) / totalContracts;
        double avgDurationDays = contracts.stream()
                .filter(contract -> contract.getStartDate() != null && contract.getEndDate() != null)
                .mapToLong(contract -> Math.max(0L, ChronoUnit.DAYS.between(contract.getStartDate(), contract.getEndDate())))
                .average()
                .orElse(0.0);

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (ContractStatus status : ContractStatus.values()) {
            long count = contracts.stream().filter(contract -> contract.getStatus() == status).count();
            if (count > 0) {
                byStatus.put(status.name(), count);
            }
        }

        return ContractAnalyticsDTO.builder()
                .totalContracts(totalContracts)
                .avgValue(avgValue)
                .completionRate(completionRate)
                .avgDurationDays(avgDurationDays)
                .byStatus(byStatus)
                .build();
    }

    private boolean usesPostgresDatabase() {
        try (var connection = dataSource.getConnection()) {
            return "PostgreSQL".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName());
        } catch (Exception ex) {
            return false;
        }
    }
}
