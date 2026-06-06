package com.team01.freelance.user.service;

import com.team01.freelance.common.observer.EventSubject;
import com.team01.freelance.user.model.User;
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
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

class UserServiceTest {

    private UserService userService;
    private UserRepository userRepository;
    private EventSubject authEventSubject;

    @BeforeEach
    void setUp() throws Exception {
        userService = new UserService();
        userRepository = mock(UserRepository.class);
        authEventSubject = mock(EventSubject.class);
        ReflectionTestUtils.setField(userService, "userRepository", userRepository);
        ReflectionTestUtils.setField(userService, "authEventSubject", authEventSubject);
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
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.countActiveContractsForUser(1L)).thenReturn(1L);

        assertThrows(IllegalStateException.class, () -> userService.deactivateUser(1L));

        verify(userRepository, never()).withdrawSubmittedProposalsForUser(1L);
        verify(userRepository, never()).save(user);
    }

    @Test
    void deactivateUserSetsStatusAndWithdrawsSubmittedProposals() {
        User user = new User();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.countActiveContractsForUser(1L)).thenReturn(0L);
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
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.countActiveContractsForUser(1L)).thenReturn(1L, 0L);
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
        when(userRepository.findTopFreelancersByEarnings(
                startDate.atStartOfDay(),
                endDate.atTime(LocalTime.MAX),
                2)).thenReturn(List.of(
                        new Object[]{2L, "User B", new BigDecimal("8000"), BigInteger.valueOf(2)},
                        new Object[]{1L, "User A", new BigDecimal("3000"), BigInteger.ONE}));

        List<TopFreelancerDTO> result = userService.getTopFreelancersByEarnings(startDate, endDate, 2);

        assertEquals(2, result.size());
        assertEquals("User B", result.get(0).getName());
        assertEquals(new BigDecimal("8000"), result.get(0).getTotalEarnings());
        assertEquals(2L, result.get(0).getContractCount());
        assertEquals("User A", result.get(1).getName());
        assertEquals(new BigDecimal("3000"), result.get(1).getTotalEarnings());
    }

    @Test
    void getTopFreelancersReturnsEmptyListWhenNoContractsExist() {
        LocalDate startDate = LocalDate.parse("2026-04-01");
        LocalDate endDate = LocalDate.parse("2026-04-30");
        when(userRepository.findTopFreelancersByEarnings(
                startDate.atStartOfDay(),
                endDate.atTime(LocalTime.MAX),
                2)).thenReturn(List.of());

        List<TopFreelancerDTO> result = userService.getTopFreelancersByEarnings(startDate, endDate, 2);

        assertEquals(0, result.size());
    }

    @Test
    void getTopFreelancersThrowsWhenStartDateIsAfterEndDate() {
        assertThrows(IllegalArgumentException.class, () -> userService.getTopFreelancersByEarnings(
                LocalDate.parse("2026-03-31"),
                LocalDate.parse("2026-03-01"),
                2));
    }

    @Test
    void getTopFreelancersThrowsWhenLimitIsNotPositive() {
        assertThrows(IllegalArgumentException.class, () -> userService.getTopFreelancersByEarnings(
                LocalDate.parse("2026-03-01"),
                LocalDate.parse("2026-03-31"),
                0));
    }

    @Test
    void getTopFreelancersUsesInclusiveDateRange() {
        LocalDate startDate = LocalDate.parse("2026-03-01");
        LocalDate endDate = LocalDate.parse("2026-03-31");
        when(userRepository.findTopFreelancersByEarnings(
                startDate.atStartOfDay(),
                endDate.atTime(LocalTime.MAX),
                10)).thenReturn(List.of());

        userService.getTopFreelancersByEarnings(startDate, endDate, null);

        verify(userRepository).findTopFreelancersByEarnings(
                LocalDateTime.parse("2026-03-01T00:00:00"),
                endDate.atTime(LocalTime.MAX),
                10);
    }
}
