package com.team01.freelance.contract.service;

import com.team01.freelance.contract.model.Contract;
import com.team01.freelance.contract.feign.UserServiceClient;
import com.team01.freelance.contract.repository.ContractRepository;
import com.team01.freelance.contract.dto.UserDTO;
import feign.FeignException;
import feign.Request;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Optional;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContractServiceF1Test {

    private ContractService contractService;
    private ContractRepository contractRepository;
    private UserServiceClient userServiceClient;

    @BeforeEach
    void setUp() throws Exception {
        contractService = new ContractService();
        contractRepository = mock(ContractRepository.class);
        userServiceClient = mock(UserServiceClient.class);
        ReflectionTestUtils.setField(contractService, "contractRepository", contractRepository);
        ReflectionTestUtils.setField(contractService, "userServiceClient", userServiceClient);
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        ReflectionTestUtils.setField(contractService, "dataSource", dataSource);
    }

    @Test
    void getActiveContractForUserReturnsMostRecentActiveContract() {
        Contract contract = new Contract();
        contract.setId(77L);

        when(userServiceClient.getUser(10L)).thenReturn(new UserDTO());
        when(contractRepository.findMostRecentActiveContractForUser(10L)).thenReturn(Optional.of(contract));

        Contract result = contractService.getActiveContractForUser(10L);

        assertEquals(77L, result.getId());
    }

    @Test
    void getActiveContractForUserThrowsWhenUserMissing() {
        when(userServiceClient.getUser(404L)).thenThrow(notFound());

        assertThrows(EntityNotFoundException.class, () -> contractService.getActiveContractForUser(404L));
    }

    private FeignException.NotFound notFound() {
        Request request = Request.create(Request.HttpMethod.GET, "/api/users/404", Map.of(), null, null, null);
        return new FeignException.NotFound("missing", request, null, Map.of());
    }
}
