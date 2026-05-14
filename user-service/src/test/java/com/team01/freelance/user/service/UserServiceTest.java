package com.team01.freelance.user.service;

import com.team01.freelance.user.model.User;
import com.team01.freelance.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

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
}
