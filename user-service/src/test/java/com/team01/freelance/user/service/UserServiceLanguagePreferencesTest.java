package com.team01.freelance.user.service;

import com.team01.freelance.user.model.User;
import com.team01.freelance.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceLanguagePreferencesTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void findUsersByLanguageAndMinimumCompletedContracts_validInput_returnsMatchingUsers() {
        User user = new User();
        user.setId(1L);
        user.setName("Omar Taha");

        when(userRepository.findUsersByLanguageAndMinimumCompletedContracts("en", 2L))
                .thenReturn(List.of(user));

        List<User> result = userService.findUsersByLanguageAndMinimumCompletedContracts(" en ", 2L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals("Omar Taha", result.get(0).getName());

        verify(userRepository).findUsersByLanguageAndMinimumCompletedContracts("en", 2L);
    }

    @Test
    void findUsersByLanguageAndMinimumCompletedContracts_blankLanguage_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> userService.findUsersByLanguageAndMinimumCompletedContracts("   ", 2L));
    }

    @Test
    void findUsersByLanguageAndMinimumCompletedContracts_nullMinimumContracts_defaultsToZero() {
        when(userRepository.findUsersByLanguageAndMinimumCompletedContracts("en", 0L))
                .thenReturn(List.of());

        List<User> result = userService.findUsersByLanguageAndMinimumCompletedContracts("en", null);

        assertEquals(0, result.size());

        verify(userRepository).findUsersByLanguageAndMinimumCompletedContracts("en", 0L);
    }
}