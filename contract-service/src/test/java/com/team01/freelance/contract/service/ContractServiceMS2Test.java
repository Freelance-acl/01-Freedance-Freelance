package com.team01.freelance.contract.service;

import com.team01.freelance.contract.cache.RedisCacheService;
import com.team01.freelance.contract.dto.ContractAnalyticsDTO;
import com.team01.freelance.contract.event.EntityObserver;
import com.team01.freelance.contract.model.Contract;
import com.team01.freelance.contract.model.ContractStatus;
import com.team01.freelance.contract.repository.ContractRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ContractServiceMS2Test {

    private ContractService contractService;
    private ContractRepository contractRepository;
    private RedisCacheService redisCacheService;
    private EntityObserver observer;

    @BeforeEach
    void setUp() {
        contractService = new ContractService();
        contractRepository = mock(ContractRepository.class);
        redisCacheService = mock(RedisCacheService.class);
        observer = mock(EntityObserver.class);

        ReflectionTestUtils.setField(contractService, "contractRepository", contractRepository);
        ReflectionTestUtils.setField(contractService, "redisCacheService", redisCacheService);
        contractService.register(observer);
    }

    @Test
    void updateContractProgressEmitsObserverEventAndEvictsAnalyticsCache() {
        Contract contract = new Contract();
        contract.setId(5L);
        contract.setMetadata(new LinkedHashMap<>(Map.of(
                "existingKey", "keep",
                "progressPercentage", 10
        )));

        when(contractRepository.findById(5L)).thenReturn(Optional.of(contract));
        when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> invocation.getArgument(0));

        contractService.updateContractProgress(5L, Map.of(
                "progressPercentage", 50,
                "lastActivityDate", "2026-03-15"
        ));

        verify(observer).onEvent(eq("PROGRESS_UPDATED"), any());
        verify(redisCacheService).evictByPrefix("contract-service::S4-F10::");
    }

    @Test
    void getContractAnalyticsMapsAggregateRowsToBuilderDto() {
        ReflectionTestUtils.setField(contractService, "redisCacheService", null);

        when(contractRepository.getContractAnalyticsSummary(
                LocalDateTime.of(2026, 3, 1, 0, 0),
                LocalDateTime.of(2026, 4, 1, 0, 0)
        )).thenReturn(List.<Object[]>of(new Object[]{2L, 2000.0, 1L, 10.0}));
        when(contractRepository.getContractCountsByStatus(
                LocalDateTime.of(2026, 3, 1, 0, 0),
                LocalDateTime.of(2026, 4, 1, 0, 0)
        )).thenReturn(List.of(
                new Object[]{"ACTIVE", 1L},
                new Object[]{"COMPLETED", 1L}
        ));

        ContractAnalyticsDTO dto = contractService.getContractAnalytics(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31)
        );

        assertEquals(2L, dto.getTotalContracts());
        assertEquals(2000.0, dto.getAverageContractValue());
        assertEquals(0.5, dto.getCompletionRate());
        assertEquals(10.0, dto.getAverageContractDurationDays());
        assertEquals(1L, dto.getContractsByStatus().get("ACTIVE"));
        assertEquals(1L, dto.getContractsByStatus().get("COMPLETED"));
    }

    @Test
    void getContractAnalyticsUsesRedisCacheAndLogsEveryInvocation() {
        ContractAnalyticsDTO cachedDto = ContractAnalyticsDTO.builder()
                .totalContracts(9L)
                .averageContractValue(1234.0)
                .completionRate(0.75)
                .averageContractDurationDays(14.0)
                .contractsByStatus(Map.of("ACTIVE", 3L, "COMPLETED", 6L))
                .build();

        when(redisCacheService.getOrCompute(
                eq("contract-service::S4-F10::2026-03-01::2026-03-31"),
                eq(ContractAnalyticsDTO.class),
                eq(Duration.ofMinutes(10)),
                any()
        )).thenReturn(cachedDto);

        ContractAnalyticsDTO first = contractService.getContractAnalytics(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31)
        );
        ContractAnalyticsDTO second = contractService.getContractAnalytics(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31)
        );

        assertSame(cachedDto, first);
        assertSame(cachedDto, second);
        verify(observer, times(2)).onEvent(eq("ANALYTICS_VIEWED"), any());
        verify(redisCacheService, times(2)).getOrCompute(
                eq("contract-service::S4-F10::2026-03-01::2026-03-31"),
                eq(ContractAnalyticsDTO.class),
                eq(Duration.ofMinutes(10)),
                any()
        );
        verifyNoInteractions(contractRepository);
    }

    @Test
    void createContractEvictsAnalyticsCacheAfterSave() {
        Contract contract = new Contract();
        contract.setJobId(40L);
        contract.setFreelancerId(50L);
        contract.setClientId(60L);
        contract.setProposalId(70L);
        contract.setAgreedAmount(1500.0);
        contract.setStartDate(LocalDateTime.of(2026, 3, 7, 9, 0));
        contract.setStatus(ContractStatus.ACTIVE);

        when(contractRepository.save(contract)).thenReturn(contract);

        contractService.createContract(contract);

        verify(redisCacheService).evictByPrefix("contract-service::S4-F10::");
    }

    @Test
    void deleteContractByIdEvictsAnalyticsCacheOnSuccess() {
        contractService.deleteContractById(77L);

        verify(contractRepository).deleteById(77L);
        verify(redisCacheService).evictByPrefix("contract-service::S4-F10::");
    }
}
