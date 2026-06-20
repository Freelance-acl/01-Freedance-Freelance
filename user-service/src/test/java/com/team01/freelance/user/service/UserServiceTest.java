package com.team01.freelance.user.service;

import com.team01.freelance.common.observer.EventSubject;
import com.team01.freelance.user.dto.UserContractSummaryDTO;
import com.team01.freelance.user.feign.ContractServiceClient;
import com.team01.freelance.user.feign.WalletServiceClient;
import com.team01.freelance.user.model.User;
import com.team01.freelance.user.model.UserRole;
import com.team01.freelance.user.model.UserStatus;
import com.team01.freelance.user.dto.TopFreelancerDTO;
import com.team01.freelance.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

class UserServiceTest {

    private UserService userService;
    private UserRepository userRepository;
    private EventSubject authEventSubject;
    private WalletServiceClient walletServiceClient;
    private ContractServiceClient contractServiceClient;

    @BeforeEach
    void setUp() throws Exception {
        userService = new UserService();
        userRepository = mock(UserRepository.class);
        authEventSubject = mock(EventSubject.class);
        walletServiceClient = mock(WalletServiceClient.class);
        contractServiceClient = mock(ContractServiceClient.class);
        ReflectionTestUtils.setField(userService, "userRepository", userRepository);
        ReflectionTestUtils.setField(userService, "authEventSubject", authEventSubject);
        ReflectionTestUtils.setField(userService, "walletServiceClient", walletServiceClient);
        ReflectionTestUtils.setField(userService, "contractServiceClient", contractServiceClient);
        stubPostgresDatabase();
    }

    private void stubPostgresDatabase() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        ReflectionTestUtils.setField(userService, "dataSource", dataSource);
    }

    @Test
    void deactivateUserThrowsNotFoundWhenUserDoesNotExist() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userService.deactivateUser(1L));
    }

    @Test
    void deactivateUserThrowsWhenActiveContractExists() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(contractServiceClient.getActiveContractCount(1L)).thenReturn(1);

        assertThrows(IllegalStateException.class, () -> userService.deactivateUser(1L));

        verify(userRepository, never()).withdrawSubmittedProposalsForUser(1L);
        verify(userRepository, never()).save(user);
    }

    @Test
    void deactivateUserSetsStatusAndWithdrawsSubmittedProposals() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(contractServiceClient.getActiveContractCount(1L)).thenReturn(0);
        when(userRepository.save(user)).thenReturn(user);

        User result = userService.deactivateUser(1L);

        assertEquals(UserStatus.DEACTIVATED, result.getStatus());
        verify(userRepository).withdrawSubmittedProposalsForUser(1L);
        verify(userRepository).save(user);
    }

    @Test
    void deactivateUserNotifiesUserDeactivatedAuthEvent() {
        User user = new User();
        user.setId(1L);
        user.setEmail("deact@test.dev");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.countActiveContractsForUser(1L)).thenReturn(0L);
        when(userRepository.save(user)).thenReturn(user);

        userService.deactivateUser(1L);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(authEventSubject).notifyObservers(eq("USER_DEACTIVATED"), payloadCaptor.capture());
        Map<?, ?> payload = assertInstanceOf(Map.class, payloadCaptor.getValue());
        assertEquals(1L, payload.get("userId"));
        assertEquals("USER_DEACTIVATED", payload.get("action"));

        Map<?, ?> details = assertInstanceOf(Map.class, payload.get("details"));
        assertEquals("deact@test.dev", details.get("email"));
    }

    @Test
    void deactivateUserFollowsPdfScenario() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(contractServiceClient.getActiveContractCount(1L)).thenReturn(1, 0);
        when(userRepository.save(user)).thenReturn(user);

        assertThrows(IllegalStateException.class, () -> userService.deactivateUser(1L));

        User result = userService.deactivateUser(1L);

        assertEquals(UserStatus.DEACTIVATED, result.getStatus());
        verify(userRepository).withdrawSubmittedProposalsForUser(1L);
        verify(userRepository).save(user);
    }

    @Test
    void findUsersByPreferenceReturnsMatchingUsers() {
        User first = new User();
        User second = new User();
        when(userRepository.findByPreference("language", "ar")).thenReturn(List.of(first, second));

        List<User> result = userService.findUsersByPreference("language", "ar");

        assertEquals(2, result.size());
        verify(userRepository).findByPreference("language", "ar");
    }

    @Test
    void findUsersByPreferenceReturnsEmptyListWhenNoUsersMatch() {
        when(userRepository.findByPreference("language", "fr")).thenReturn(List.of());

        List<User> result = userService.findUsersByPreference("language", "fr");

        assertEquals(0, result.size());
        verify(userRepository).findByPreference("language", "fr");
    }

    @Test
    void findUsersByPreferenceThrowsWhenKeyIsBlank() {
        assertThrows(IllegalArgumentException.class, () -> userService.findUsersByPreference("", "ar"));
    }

    @Test
    void findUsersByPreferenceThrowsWhenValueIsBlank() {
        assertThrows(IllegalArgumentException.class, () -> userService.findUsersByPreference("language", " "));
    }

    @Test
    void getTopFreelancersReturnsRequestedLimitInEarningsOrder() {
        LocalDate startDate = LocalDate.parse("2026-03-01");
        LocalDate endDate = LocalDate.parse("2026-03-31");
        User userA = freelancer(1L, "User A");
        User userB = freelancer(2L, "User B");
        User userC = freelancer(3L, "User C");
        when(userRepository.findByRole(UserRole.FREELANCER)).thenReturn(List.of(userA, userB, userC));
        when(walletServiceClient.getFreelancerTotalEarnings(1L, startDate, endDate)).thenReturn(new BigDecimal("3000"));
        when(walletServiceClient.getFreelancerTotalEarnings(2L, startDate, endDate)).thenReturn(new BigDecimal("8000"));
        when(walletServiceClient.getFreelancerTotalEarnings(3L, startDate, endDate)).thenReturn(new BigDecimal("5000"));
        when(contractServiceClient.getUserContractSummary(1L))
                .thenReturn(contractSummary(1L, "User A", 1L));
        when(contractServiceClient.getUserContractSummary(2L))
                .thenReturn(contractSummary(2L, "User B", 2L));
        when(contractServiceClient.getUserContractSummary(3L))
                .thenReturn(contractSummary(3L, "User C", 1L));

        List<TopFreelancerDTO> result = userService.getTopFreelancersByEarnings(startDate, endDate, 2);

        assertEquals(2, result.size());
        assertEquals("User B", result.get(0).getName());
        assertEquals(new BigDecimal("8000"), result.get(0).getTotalEarnings());
        assertEquals(2L, result.get(0).getContractCount());
        assertEquals("User C", result.get(1).getName());
        assertEquals(new BigDecimal("5000"), result.get(1).getTotalEarnings());
        assertEquals(1L, result.get(1).getContractCount());

        verify(userRepository).findByRole(UserRole.FREELANCER);
        verify(walletServiceClient).getFreelancerTotalEarnings(1L, startDate, endDate);
        verify(walletServiceClient).getFreelancerTotalEarnings(2L, startDate, endDate);
        verify(walletServiceClient).getFreelancerTotalEarnings(3L, startDate, endDate);
        verify(contractServiceClient).getUserContractSummary(1L);
        verify(contractServiceClient).getUserContractSummary(2L);
        verify(contractServiceClient).getUserContractSummary(3L);
    }

    @Test
    void getTopFreelancersReturnsEmptyListWhenNoFreelancerHasPayoutsOrCompletedContracts() {
        LocalDate startDate = LocalDate.parse("2026-04-01");
        LocalDate endDate = LocalDate.parse("2026-04-30");
        User user = freelancer(1L, "User A");
        when(userRepository.findByRole(UserRole.FREELANCER)).thenReturn(List.of(user));
        when(walletServiceClient.getFreelancerTotalEarnings(1L, startDate, endDate)).thenReturn(BigDecimal.ZERO);
        when(contractServiceClient.getUserContractSummary(1L))
                .thenReturn(contractSummary(1L, "User A", 0L));

        List<TopFreelancerDTO> result = userService.getTopFreelancersByEarnings(startDate, endDate, 2);

        assertEquals(0, result.size());
    }

    @Test
    void getTopFreelancersThrowsWhenStartDateIsAfterEndDate() {
        assertThrows(IllegalArgumentException.class, () -> userService.getTopFreelancersByEarnings(
                LocalDate.parse("2026-03-31"),
                LocalDate.parse("2026-03-01"),
                2));
        verifyNoInteractions(walletServiceClient, contractServiceClient);
    }

    @Test
    void getTopFreelancersThrowsWhenLimitIsNotPositive() {
        assertThrows(IllegalArgumentException.class, () -> userService.getTopFreelancersByEarnings(
                LocalDate.parse("2026-03-01"),
                LocalDate.parse("2026-03-31"),
                0));
        verifyNoInteractions(walletServiceClient, contractServiceClient);
    }

    @Test
    void getTopFreelancersUsesDefaultLimitWhenLimitIsNull() {
        LocalDate startDate = LocalDate.parse("2026-03-01");
        LocalDate endDate = LocalDate.parse("2026-03-31");
        when(userRepository.findByRole(UserRole.FREELANCER)).thenReturn(List.of(
                freelancer(1L, "One"),
                freelancer(2L, "Two"),
                freelancer(3L, "Three"),
                freelancer(4L, "Four"),
                freelancer(5L, "Five"),
                freelancer(6L, "Six"),
                freelancer(7L, "Seven"),
                freelancer(8L, "Eight"),
                freelancer(9L, "Nine"),
                freelancer(10L, "Ten"),
                freelancer(11L, "Eleven")));

        for (long id = 1; id <= 11; id++) {
            when(walletServiceClient.getFreelancerTotalEarnings(id, startDate, endDate))
                    .thenReturn(BigDecimal.valueOf(id));
            when(contractServiceClient.getUserContractSummary(id))
                    .thenReturn(contractSummary(id, "User " + id, 1L));
        }

        List<TopFreelancerDTO> result = userService.getTopFreelancersByEarnings(startDate, endDate, null);

        assertEquals(10, result.size());
        assertEquals(11L, result.get(0).getUserId());
        assertEquals(2L, result.get(9).getUserId());
    }

    @Test
    void getTopFreelancersTreatsFailedClientCallsAsZeroMetrics() {
        LocalDate startDate = LocalDate.parse("2026-03-01");
        LocalDate endDate = LocalDate.parse("2026-03-31");
        User userA = freelancer(1L, "User A");
        User userB = freelancer(2L, "User B");
        when(userRepository.findByRole(UserRole.FREELANCER)).thenReturn(List.of(userA, userB));
        when(walletServiceClient.getFreelancerTotalEarnings(1L, startDate, endDate))
                .thenThrow(new RuntimeException("wallet down"));
        when(contractServiceClient.getUserContractSummary(1L))
                .thenThrow(new RuntimeException("contract down"));
        when(walletServiceClient.getFreelancerTotalEarnings(2L, startDate, endDate))
                .thenReturn(new BigDecimal("2000"));
        when(contractServiceClient.getUserContractSummary(2L))
                .thenReturn(contractSummary(2L, "User B", 3L));

        List<TopFreelancerDTO> result = userService.getTopFreelancersByEarnings(startDate, endDate, 10);

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getUserId());
        assertEquals(new BigDecimal("2000"), result.get(0).getTotalEarnings());
        assertEquals(3L, result.get(0).getContractCount());
    }

    private static User freelancer(Long id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setRole(UserRole.FREELANCER);
        return user;
    }

    private static UserContractSummaryDTO contractSummary(Long userId, String name, Long completedContracts) {
        return UserContractSummaryDTO.builder()
                .userId(userId)
                .name(name)
                .totalContracts(completedContracts)
                .completedContracts(completedContracts)
                .terminatedContracts(0L)
                .totalEarnings(0.0)
                .averageContractValue(0.0)
                .build();
    }
}
