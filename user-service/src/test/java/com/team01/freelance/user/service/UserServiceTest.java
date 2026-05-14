package com.team01.freelance.user.service;

import com.team01.freelance.user.dto.TopFreelancerDTO;
import com.team01.freelance.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private UserService userService;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userService = new UserService();
        userRepository = mock(UserRepository.class);
        ReflectionTestUtils.setField(userService, "userRepository", userRepository);
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
