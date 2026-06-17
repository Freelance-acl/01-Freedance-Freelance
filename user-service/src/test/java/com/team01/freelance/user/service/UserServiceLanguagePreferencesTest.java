package com.team01.freelance.user.service;

import com.team01.freelance.user.dto.UserContractSummaryDTO;
import com.team01.freelance.user.feign.ContractServiceClient;
import com.team01.freelance.user.model.User;
import com.team01.freelance.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceLanguagePreferencesTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ContractServiceClient contractServiceClient;

    @InjectMocks
    private UserService userService;

    @Mock
    private DataSource dataSource;

    @BeforeEach
    void setUp() throws Exception {
        Connection connection = org.mockito.Mockito.mock(Connection.class);
        DatabaseMetaData metaData = org.mockito.Mockito.mock(DatabaseMetaData.class);
        lenient().when(dataSource.getConnection()).thenReturn(connection);
        lenient().when(connection.getMetaData()).thenReturn(metaData);
        lenient().when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
    }

    @Test
    void findUsersByLanguageAndMinimumCompletedContracts_validInput_returnsMatchingUsers() {
        User qualified = userWithLanguage(1L, "Omar Taha", "en");
        User notEnough = userWithLanguage(2L, "Mona Ali", "en");
        User otherLanguage = userWithLanguage(3L, "Youssef Samir", "ar");
        when(userRepository.findAll()).thenReturn(List.of(qualified, notEnough, otherLanguage));
        when(contractServiceClient.getUserContractSummary(1L))
                .thenReturn(contractSummary(1L, "Omar Taha", 2L));
        when(contractServiceClient.getUserContractSummary(2L))
                .thenReturn(contractSummary(2L, "Mona Ali", 1L));

        List<User> result = userService.findUsersByLanguageAndMinimumCompletedContracts(" en ", 2L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals("Omar Taha", result.get(0).getName());

        verify(userRepository).findAll();
        verify(contractServiceClient).getUserContractSummary(1L);
        verify(contractServiceClient).getUserContractSummary(2L);
    }

    @Test
    void findUsersByLanguageAndMinimumCompletedContracts_blankLanguage_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> userService.findUsersByLanguageAndMinimumCompletedContracts("   ", 2L));
        verifyNoInteractions(contractServiceClient);
    }

    @Test
    void findUsersByLanguageAndMinimumCompletedContracts_nullMinimumContracts_defaultsToZero() {
        User user = userWithLanguage(1L, "Omar Taha", "en");
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<User> result = userService.findUsersByLanguageAndMinimumCompletedContracts("en", null);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());

        verify(userRepository).findAll();
        verifyNoInteractions(contractServiceClient);
    }

    @Test
    void findUsersByLanguageAndMinimumCompletedContracts_contractClientFailureCountsAsZero() {
        User user = userWithLanguage(1L, "Omar Taha", "en");
        when(userRepository.findAll()).thenReturn(List.of(user));
        when(contractServiceClient.getUserContractSummary(1L))
                .thenThrow(new RuntimeException("contract service unavailable"));

        List<User> result = userService.findUsersByLanguageAndMinimumCompletedContracts("en", 1L);

        assertEquals(0, result.size());

        verify(userRepository).findAll();
        verify(contractServiceClient).getUserContractSummary(1L);
    }

    private static User userWithLanguage(Long id, String name, String language) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setPreferences(Map.of("language", language));
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
