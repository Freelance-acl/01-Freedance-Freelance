package com.team01.freelance.contract.service;

import com.team01.freelance.contract.feign.JobServiceClient;
import com.team01.freelance.contract.feign.UserServiceClient;
import com.team01.freelance.contract.model.Contract;
import com.team01.freelance.contract.model.ContractStatus;
import com.team01.freelance.contract.repository.ContractRepository;
import com.team01.freelance.contract.dto.UserContractSummaryDTO;
import com.team01.freelance.contract.dto.UserDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContractServiceClientFallbackTest {

    private ContractService contractService;
    private ContractRepository contractRepository;
    private JdbcTemplate jdbcTemplate;
    private UserServiceClient userServiceClient;
    private JobServiceClient jobServiceClient;

    @BeforeEach
    void setUp() throws Exception {
        contractService = new ContractService();
        contractRepository = mock(ContractRepository.class);
        jdbcTemplate = mock(JdbcTemplate.class);
        userServiceClient = mock(UserServiceClient.class);
        jobServiceClient = mock(JobServiceClient.class);

        ReflectionTestUtils.setField(contractService, "contractRepository", contractRepository);
        ReflectionTestUtils.setField(contractService, "jdbcTemplate", jdbcTemplate);
        ReflectionTestUtils.setField(contractService, "dataSource", postgresDataSource());
        ReflectionTestUtils.setField(contractService, "userServiceClient", userServiceClient);
        ReflectionTestUtils.setField(contractService, "jobServiceClient", jobServiceClient);
    }

    @Test
    void userSummaryFallsBackToLocalUserNameWhenUserServiceReadFails() {
        when(userServiceClient.getUser(10L)).thenThrow(new RuntimeException("user-service unavailable"));
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM users WHERE id = ?"), eq(Integer.class), eq(10L)))
                .thenReturn(1);
        when(jdbcTemplate.queryForObject(eq("SELECT name FROM users WHERE id = ?"), eq(String.class), eq(10L)))
                .thenReturn("Fallback Freelancer");
        when(contractRepository.countContractsForUser(10L)).thenReturn(3L);
        when(contractRepository.countCompletedContractsForUser(10L)).thenReturn(2L);
        when(contractRepository.countTerminatedContractsForUser(10L)).thenReturn(1L);
        when(contractRepository.sumCompletedContractEarningsForUser(10L)).thenReturn(2400.0);

        UserContractSummaryDTO summary = contractService.getUserContractSummary(10L);

        assertEquals("Fallback Freelancer", summary.getName());
        assertEquals(3L, summary.getTotalContracts());
        assertEquals(800.0, summary.getAverageContractValue());
    }

    @Test
    void searchContractsFallsBackToLocalJobTitleWhenJobServiceReadFails() {
        Contract contract = contract(1L, 10L, 20L);
        UserDTO user = new UserDTO();
        user.setName("Remote Freelancer");

        when(contractRepository.searchContracts(100.0, 2000.0, "ACTIVE")).thenReturn(List.of(contract));
        when(userServiceClient.getUser(10L)).thenReturn(user);
        when(jobServiceClient.getJob(20L)).thenThrow(new RuntimeException("job-service unavailable"));
        when(jdbcTemplate.queryForObject(eq("SELECT title FROM jobs WHERE id = ?"), eq(String.class), eq(20L)))
                .thenReturn("Fallback Job");

        var result = contractService.searchContracts(100.0, 2000.0, "ACTIVE");

        assertEquals(1, result.size());
        assertEquals("Remote Freelancer", result.get(0).getFreelancerName());
        assertEquals("Fallback Job", result.get(0).getJobTitle());
    }

    private DataSource postgresDataSource() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getDatabaseProductName()).thenReturn("PostgreSQL");
        return dataSource;
    }

    private Contract contract(Long id, Long freelancerId, Long jobId) {
        Contract contract = new Contract();
        contract.setId(id);
        contract.setFreelancerId(freelancerId);
        contract.setJobId(jobId);
        contract.setClientId(30L);
        contract.setProposalId(40L);
        contract.setAgreedAmount(1000.0);
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setStartDate(LocalDateTime.of(2026, 3, 1, 9, 0));
        contract.setCreatedAt(LocalDateTime.of(2026, 3, 1, 9, 0));
        return contract;
    }
}
