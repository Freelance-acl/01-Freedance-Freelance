package com.team01.freelance.user.controller;

import com.team01.freelance.user.model.User;
import com.team01.freelance.user.model.UserRole;
import com.team01.freelance.user.model.UserStatus;
import com.team01.freelance.user.repository.UserRepository;
import com.team01.freelance.user.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.test.context.support.WithMockUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [S1-F2] Integration tests for {@code PUT /api/users/{id}/preferences}.
 */
@Transactional
@WithMockUser(roles = "ADMIN")
class UpdateUserPreferencesIntegrationTest extends AbstractIntegrationTest {

    private static final String PREFERENCES_URL = "/api/users/{id}/preferences";

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(webApplicationContext);

        Map<String, Object> preferences = new LinkedHashMap<>();
        preferences.put("language", "en");
        preferences.put("timezone", "UTC");

        user = new User();
        user.setName("Pref User");
        user.setEmail("pref-" + System.nanoTime() + "@test.dev");
        user.setPassword("secret");
        user.setPhone("+1999" + (System.nanoTime() % 1_000_000_000L));
        user.setRole(UserRole.FREELANCER);
        user.setStatus(UserStatus.ACTIVE);
        user.setPreferences(preferences);
        user = userRepository.save(user);
    }

    /**
     * Spec scenarios (a)+(b)+(c): merge keeps existing keys, updates overlaps, adds new keys.
     */
    @Test
    void updatePreferences_mergesIntoExistingMap() throws Exception {
        mockMvc.perform(put(PREFERENCES_URL, user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"timezone":"Africa/Cairo","hourlyRate":45}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferences.language").value("en"))
                .andExpect(jsonPath("$.preferences.timezone").value("Africa/Cairo"))
                .andExpect(jsonPath("$.preferences.hourlyRate").value(45))
                .andExpect(jsonPath("$.password").doesNotExist());

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertEquals("en", updated.getPreferences().get("language"));
        assertEquals("Africa/Cairo", updated.getPreferences().get("timezone"));
        assertEquals(45, ((Number) updated.getPreferences().get("hourlyRate")).intValue());
    }

    /** Spec scenario (d): unknown user → 404. */
    @Test
    void updatePreferences_unknownUser_returns404() throws Exception {
        mockMvc.perform(put(PREFERENCES_URL, 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"timezone":"Africa/Cairo"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void updatePreferences_nullInitialPreferences_addsAllKeys() throws Exception {
        User noPrefs = new User();
        noPrefs.setName("No Prefs");
        noPrefs.setEmail("noprefs-" + System.nanoTime() + "@test.dev");
        noPrefs.setPassword("secret");
        noPrefs.setPhone("+1888" + (System.nanoTime() % 1_000_000_000L));
        noPrefs.setRole(UserRole.CLIENT);
        noPrefs.setStatus(UserStatus.ACTIVE);
        noPrefs = userRepository.save(noPrefs);

        mockMvc.perform(put(PREFERENCES_URL, noPrefs.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"language":"fr","theme":"dark"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferences.language").value("fr"))
                .andExpect(jsonPath("$.preferences.theme").value("dark"));
    }
}