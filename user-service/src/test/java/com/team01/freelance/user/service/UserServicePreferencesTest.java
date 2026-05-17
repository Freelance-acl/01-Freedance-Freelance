package com.team01.freelance.user.service;


import com.team01.freelance.user.model.User;
import com.team01.freelance.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UserService#updatePreferences(Long, Map)}.
 */
@ExtendWith(MockitoExtension.class)
class UserServicePreferencesTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void updatePreferences_mergesWithoutDroppingExistingKeys() {
        Long id = 1L;
        Map<String, Object> existing = new LinkedHashMap<>();
        existing.put("language", "en");
        existing.put("timezone", "UTC");

        User user = new User();
        user.setId(id);
        user.setPreferences(existing);

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> incoming = Map.of(
                "timezone", "Africa/Cairo",
                "hourlyRate", 45);

        User result = userService.updatePreferences(id, incoming);

        assertEquals("en", result.getPreferences().get("language"));
        assertEquals("Africa/Cairo", result.getPreferences().get("timezone"));
        assertEquals(45, result.getPreferences().get("hourlyRate"));
        verify(userRepository).save(user);
    }

    @Test
    void updatePreferences_userNotFound_throws404() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> userService.updatePreferences(999L, Map.of("language", "fr")));
    }
}
