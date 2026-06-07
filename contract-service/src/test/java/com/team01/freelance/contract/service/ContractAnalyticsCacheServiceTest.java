package com.team01.freelance.contract.service;

import com.team01.freelance.contract.dto.ContractAnalyticsDTO;
import com.team01.freelance.contract.model.Contract;
import com.team01.freelance.contract.model.ContractStatus;
import com.team01.freelance.contract.repository.ContractRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContractAnalyticsCacheServiceTest {

    private ContractAnalyticsCacheService analyticsCacheService;
    private ContractRepository contractRepository;

    @BeforeEach
    void setUp() throws Exception {
        contractRepository = mock(ContractRepository.class);
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        var metaData = mock(java.sql.DatabaseMetaData.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("H2");
        analyticsCacheService = new ContractAnalyticsCacheService(contractRepository, dataSource);
    }

    @Test
    void getContractAnalyticsFallsBackToInMemoryAggregation() {
        Contract active = new Contract();
        active.setAgreedAmount(1000.0);
        active.setStatus(ContractStatus.ACTIVE);
        active.setStartDate(LocalDateTime.of(2026, 1, 1, 9, 0));
        active.setEndDate(LocalDateTime.of(2026, 1, 4, 9, 0));

        Contract completed = new Contract();
        completed.setAgreedAmount(3000.0);
        completed.setStatus(ContractStatus.COMPLETED);
        completed.setStartDate(LocalDateTime.of(2026, 1, 2, 9, 0));
        completed.setEndDate(LocalDateTime.of(2026, 1, 6, 9, 0));

        when(contractRepository.findAll()).thenReturn(List.of(active, completed));

        ContractAnalyticsDTO dto = analyticsCacheService.getContractAnalytics();

        assertEquals(2L, dto.getTotalContracts());
        assertEquals(2000.0, dto.getAvgValue());
        assertEquals(50.0, dto.getCompletionRate());
        assertEquals(3.5, dto.getAvgDurationDays());
        assertEquals(1L, dto.getByStatus().get("ACTIVE"));
        assertEquals(1L, dto.getByStatus().get("COMPLETED"));
    }
}
