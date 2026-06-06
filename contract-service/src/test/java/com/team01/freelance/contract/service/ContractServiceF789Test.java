package com.team01.freelance.contract.service;

import com.team01.freelance.contract.dto.FreelancerPerformanceDTO;
import com.team01.freelance.contract.dto.StalledContractDTO;
import com.team01.freelance.contract.repository.ContractRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Collections;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContractServiceF789Test {

    private ContractService contractService;
    private ContractRepository contractRepository;

    @BeforeEach
    void setUp() throws Exception {
        contractService = new ContractService();
        contractRepository = mock(ContractRepository.class);
        ReflectionTestUtils.setField(contractService, "contractRepository", contractRepository);
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        ReflectionTestUtils.setField(contractService, "dataSource", dataSource);
    }

    @Test
    void purgeOldContractDataReturnsDeletedCount() {
        when(contractRepository.countPurgeCandidates(any(LocalDateTime.class))).thenReturn(7L);
        when(contractRepository.purgeOldContracts(any(LocalDateTime.class))).thenReturn(7);

        long deletedCount = contractService.purgeOldContractData(30);

        assertEquals(7L, deletedCount);
        verify(contractRepository).countPurgeCandidates(any(LocalDateTime.class));
        verify(contractRepository).purgeOldContracts(any(LocalDateTime.class));
    }

    @Test
    void freelancerSummaryReturnsExpectedValues() {
        when(contractRepository.freelancerExists(10L)).thenReturn(true);
        when(contractRepository.getFreelancerPerformance(eq(10L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new Object[]{5L, 4L, 7000.0, 1400.0, 17.5});

        FreelancerPerformanceDTO dto = contractService.getFreelancerPerformanceSummary(
                10L,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31)
        );

        assertEquals(10L, dto.getFreelancerId());
        assertEquals(5L, dto.getTotalContracts());
        assertEquals(7000.0, dto.getTotalEarnings());
        assertEquals(1400.0, dto.getAverageContractValue());
        assertEquals(80.0, dto.getCompletionRate());
        assertEquals(17.5, dto.getAverageDurationDays());
    }

    @Test
    void freelancerSummaryThrowsWhenFreelancerMissing() {
        when(contractRepository.freelancerExists(999L)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () ->
                contractService.getFreelancerPerformanceSummary(
                        999L,
                        LocalDate.of(2026, 3, 1),
                        LocalDate.of(2026, 3, 31)
                )
        );
    }

    @Test
    void stalledContractsAreMappedToDto() {
        when(contractRepository.findStalledContracts(50.0, 7))
                .thenReturn(Collections.singletonList(new Object[]{1L, "Freelancer A", "Job A", 1000.0, 10.0, 30L}));

        List<StalledContractDTO> result = contractService.findStalledContracts(50.0, 7);

        assertEquals(1, result.size());
        assertEquals(1L, result.getFirst().getContractId());
        assertEquals("Freelancer A", result.getFirst().getFreelancerName());
        assertEquals("Job A", result.getFirst().getJobTitle());
        assertEquals(1000.0, result.getFirst().getAgreedAmount());
        assertEquals(10.0, result.getFirst().getProgressPercentage());
        assertEquals(30L, result.getFirst().getDaysSinceLastActivity());
    }
}
