package com.team01.freelance.contract.service;

import com.team01.freelance.contract.dto.ContractAnalyticsDTO;
import com.team01.freelance.contract.observer.EntityObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContractServiceAnalyticsTest {

    private ContractService contractService;
    private ContractAnalyticsCacheService contractAnalyticsCacheService;
    private EntityObserver observer;

    @BeforeEach
    void setUp() {
        contractService = new ContractService();
        contractAnalyticsCacheService = mock(ContractAnalyticsCacheService.class);
        observer = mock(EntityObserver.class);
        ReflectionTestUtils.setField(contractService, "contractAnalyticsCacheService", contractAnalyticsCacheService);
        ReflectionTestUtils.setField(contractService, "observers", List.of(observer));
    }

    @Test
    void getContractAnalyticsLogsAnalyticsViewedEveryCall() {
        ContractAnalyticsDTO dto = ContractAnalyticsDTO.builder()
                .totalContracts(7L)
                .avgValue(1200.0)
                .completionRate(60.0)
                .avgDurationDays(4.0)
                .byStatus(Map.of("ACTIVE", 4L, "COMPLETED", 3L))
                .build();
        when(contractAnalyticsCacheService.getContractAnalytics()).thenReturn(dto);

        ContractAnalyticsDTO result = contractService.getContractAnalytics();

        assertSame(dto, result);
        verify(observer).onEvent(eq("ANALYTICS_VIEWED"), any(Map.class));
    }
}
