package com.team01.freelance.user.controller;

import com.team01.freelance.user.dto.UserContractSummaryDTO;
import com.team01.freelance.user.feign.ContractServiceClient;
import com.team01.freelance.user.model.User;
import com.team01.freelance.user.model.UserRole;
import com.team01.freelance.user.model.UserStatus;
import com.team01.freelance.user.repository.UserRepository;
import com.team01.freelance.user.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.test.context.support.WithMockUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [S1-F9] Integration tests for {@code GET /api/users/preferences/language}.
 */
@Transactional
@WithMockUser(roles = "ADMIN")
class UserLanguagePreferencesIntegrationTest extends AbstractIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private ContractServiceClient contractServiceClient;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(webApplicationContext);
    }

    @Test
    void languageFilter_returnsUsersWithMinimumCompletedContracts() throws Exception {
        User qualified = saveUserWithLanguage("Qualified", "ar");
        User notEnough = saveUserWithLanguage("Not Enough", "ar");
        User otherLanguage = saveUserWithLanguage("Other Language", "en");
        when(contractServiceClient.getUserContractSummary(qualified.getId()))
                .thenReturn(contractSummary(qualified, 2L));
        when(contractServiceClient.getUserContractSummary(notEnough.getId()))
                .thenReturn(contractSummary(notEnough, 1L));

        mockMvc.perform(get("/api/users/preferences/language")
                        .param("lang", "ar")
                        .param("minContracts", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Qualified"));

        verify(contractServiceClient).getUserContractSummary(qualified.getId());
        verify(contractServiceClient).getUserContractSummary(notEnough.getId());
    }

    private User saveUserWithLanguage(String name, String language) {
        User user = new User();
        user.setName(name);
        user.setEmail("lang-" + System.nanoTime() + "@test.dev");
        user.setPassword("secret");
        user.setPhone("+8000" + (System.nanoTime() % 1_000_000_000L));
        user.setRole(UserRole.FREELANCER);
        user.setStatus(UserStatus.ACTIVE);
        user.setPreferences(new LinkedHashMap<>(Map.of("language", language)));
        return userRepository.save(user);
    }

    private UserContractSummaryDTO contractSummary(User user, Long completedContracts) {
        return UserContractSummaryDTO.builder()
                .userId(user.getId())
                .name(user.getName())
                .totalContracts(completedContracts)
                .completedContracts(completedContracts)
                .terminatedContracts(0L)
                .totalEarnings(0.0)
                .averageContractValue(0.0)
                .build();
    }
}
