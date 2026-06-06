package com.team01.freelance.user.service;

import com.team01.freelance.user.dto.UserContractSummaryDTO;
import com.team01.freelance.user.model.User;
import com.team01.freelance.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UserService#getUserContractSummary(Long)}.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceContractSummaryTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void getUserContractSummary_mapsAggregateRowToDto() {
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        user.setName("Freelancer");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.getUserContractSummary(userId))
                .thenReturn(new Object[]{5L, 3L, 1L, 3000.0, 1000.0});

        UserContractSummaryDTO dto = userService.getUserContractSummary(userId);

        assertEquals(userId, dto.getUserId());
        assertEquals("Freelancer", dto.getName());
        assertEquals(5L, dto.getTotalContracts());
        assertEquals(3L, dto.getCompletedContracts());
        assertEquals(1L, dto.getTerminatedContracts());
        assertEquals(3000.0, dto.getTotalEarnings());
        assertEquals(1000.0, dto.getAverageContractValue());
    }

    @Test
    void getUserContractSummary_nullAggregateRow_returnsZeros() {
        Long userId = 2L;
        User user = new User();
        user.setId(userId);
        user.setName("No Contracts");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.getUserContractSummary(userId)).thenReturn(null);

        UserContractSummaryDTO dto = userService.getUserContractSummary(userId);

        assertEquals(0L, dto.getTotalContracts());
        assertEquals(0.0, dto.getTotalEarnings());
        assertEquals(0.0, dto.getAverageContractValue());
    }

    @Test
    void getUserContractSummary_userNotFound_throws404() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> userService.getUserContractSummary(999L));
    }
}
