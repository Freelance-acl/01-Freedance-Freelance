package com.team01.freelance.contract.service;

import com.team01.freelance.contract.dto.FreelancerPerformanceDTO;
import com.team01.freelance.contract.dto.StalledContractDTO;
import com.team01.freelance.contract.feign.JobServiceClient;
import com.team01.freelance.contract.feign.UserServiceClient;
import com.team01.freelance.contract.model.Contract;
import com.team01.freelance.contract.model.ContractStatus;
import com.team01.freelance.contract.repository.ContractRepository;
import com.team01.freelance.job.model.Job;
import com.team01.freelance.user.model.User;
import feign.FeignException;
import feign.Request;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
    private UserServiceClient userServiceClient;
    private JobServiceClient jobServiceClient;

    @BeforeEach
    void setUp() throws Exception {
        contractService = new ContractService();
        contractRepository = mock(ContractRepository.class);
        userServiceClient = mock(UserServiceClient.class);
        jobServiceClient = mock(JobServiceClient.class);
        ReflectionTestUtils.setField(contractService, "contractRepository", contractRepository);
        ReflectionTestUtils.setField(contractService, "userServiceClient", userServiceClient);
        ReflectionTestUtils.setField(contractService, "jobServiceClient", jobServiceClient);
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
        when(userServiceClient.getUser(10L)).thenReturn(new User());
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
        when(userServiceClient.getUser(999L)).thenThrow(notFound());

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
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setFreelancerId(10L);
        contract.setJobId(20L);
        contract.setAgreedAmount(1000.0);
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setCreatedAt(LocalDateTime.now().minusDays(30));
        contract.setMetadata(Map.of("progressPercentage", 10.0, "lastActivityDate", LocalDateTime.now().minusDays(30).toString()));
        User user = new User();
        user.setName("Freelancer A");
        Job job = new Job();
        job.setTitle("Job A");
        when(contractRepository.findStalledContracts(50.0, 7))
                .thenReturn(List.of(contract));
        when(userServiceClient.getUser(10L)).thenReturn(user);
        when(jobServiceClient.getJob(20L)).thenReturn(job);

        List<StalledContractDTO> result = contractService.findStalledContracts(50.0, 7);

        assertEquals(1, result.size());
        assertEquals(1L, result.getFirst().getContractId());
        assertEquals("Freelancer A", result.getFirst().getFreelancerName());
        assertEquals("Job A", result.getFirst().getJobTitle());
        assertEquals(1000.0, result.getFirst().getAgreedAmount());
        assertEquals(10.0, result.getFirst().getProgressPercentage());
        assertEquals(30L, result.getFirst().getDaysSinceLastActivity());
    }

    private FeignException.NotFound notFound() {
        Request request = Request.create(Request.HttpMethod.GET, "/api/users/999", Map.of(), null, null, null);
        return new FeignException.NotFound("missing", request, null, Map.of());
    }
}
