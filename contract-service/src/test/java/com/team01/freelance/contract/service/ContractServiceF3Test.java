package com.team01.freelance.contract.service;

import com.team01.freelance.contract.dto.ContractSummaryDTO;
import com.team01.freelance.contract.feign.JobServiceClient;
import com.team01.freelance.contract.feign.UserServiceClient;
import com.team01.freelance.contract.model.Contract;
import com.team01.freelance.contract.model.ContractStatus;
import com.team01.freelance.contract.repository.ContractRepository;
import com.team01.freelance.contract.dto.JobDTO;
import com.team01.freelance.contract.dto.UserDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContractServiceF3Test {

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
    void searchContractsMapsJoinedRowsToDto() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 1, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 5, 9, 0);
        Contract contract = new Contract();
        contract.setId(9L);
        contract.setFreelancerId(10L);
        contract.setJobId(20L);
        contract.setAgreedAmount(5000.0);
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setStartDate(start);
        contract.setEndDate(end);
        UserDTO user = new UserDTO();
        user.setName("Nour");
        JobDTO job = new JobDTO();
        job.setTitle("Backend API");
        when(contractRepository.searchContracts(2000.0, 6000.0, "ACTIVE"))
                .thenReturn(List.of(contract));
        when(userServiceClient.getUser(10L)).thenReturn(user);
        when(jobServiceClient.getJob(20L)).thenReturn(job);

        List<ContractSummaryDTO> result = contractService.searchContracts(2000.0, 6000.0, "ACTIVE");

        assertEquals(1, result.size());
        assertEquals(9L, result.getFirst().getContractId());
        assertEquals("Nour", result.getFirst().getFreelancerName());
        assertEquals("Backend API", result.getFirst().getJobTitle());
        assertEquals(5000.0, result.getFirst().getAgreedAmount());
        assertEquals("ACTIVE", result.getFirst().getStatus());
        assertEquals(4L, result.getFirst().getDurationDays());
    }

    @Test
    void searchContractsRejectsUnknownStatus() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> contractService.searchContracts(100.0, 200.0, "BROKEN"));

        assertInstanceOf(IllegalArgumentException.class, ex);
    }

    @Test
    void searchContractsRejectsInvalidRange() {
        assertThrows(IllegalArgumentException.class,
                () -> contractService.searchContracts(5000.0, 1000.0, ContractStatus.ACTIVE.name()));
    }
}
