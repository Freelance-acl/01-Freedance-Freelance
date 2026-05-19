package com.team01.freelance.user.controller;

import com.team01.freelance.user.model.User;
import com.team01.freelance.user.model.UserRole;
import com.team01.freelance.user.model.UserStatus;
import com.team01.freelance.user.repository.UserRepository;
import com.team01.freelance.user.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [S1-F5] Integration tests for {@code GET /api/users/preferences/search}.
 */
@Transactional
class UserPreferenceSearchIntegrationTest extends AbstractIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void searchByPreference_returnsUsersWithMatchingLanguage() throws Exception {
        saveUserWithPreferences("Arabic User", Map.of("language", "ar"));
        saveUserWithPreferences("English User", Map.of("language", "en"));

        mockMvc.perform(get("/api/users/preferences/search")
                        .param("key", "language")
                        .param("value", "ar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Arabic User"));
    }

    private void saveUserWithPreferences(String name, Map<String, Object> preferences) {
        User user = new User();
        user.setName(name);
        user.setEmail("pref-search-" + System.nanoTime() + "@test.dev");
        user.setPassword("secret");
        user.setPhone("+6000" + (System.nanoTime() % 1_000_000_000L));
        user.setRole(UserRole.FREELANCER);
        user.setStatus(UserStatus.ACTIVE);
        user.setPreferences(new LinkedHashMap<>(preferences));
        userRepository.save(user);
    }
}
