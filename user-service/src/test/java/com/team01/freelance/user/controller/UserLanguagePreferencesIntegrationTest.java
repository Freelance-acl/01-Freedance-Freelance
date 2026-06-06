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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(webApplicationContext);
    }

    @Test
    void languageFilter_returnsUsersWithMinimumCompletedContracts() throws Exception {
        User qualified = saveUserWithLanguage("Qualified", "ar");
        User notEnough = saveUserWithLanguage("Not Enough", "ar");
        insertCompletedContracts(qualified.getId(), 2);
        insertCompletedContracts(notEnough.getId(), 1);

        mockMvc.perform(get("/api/users/preferences/language")
                        .param("lang", "ar")
                        .param("minContracts", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Qualified"));
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

    private void insertCompletedContracts(Long freelancerId, int count) {
        for (int i = 0; i < count; i++) {
            jdbcTemplate.update("""
                    INSERT INTO contracts (job_id, freelancer_id, client_id, proposal_id, agreed_amount,
                                           status, start_date, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    10L + i, freelancerId, 20L, 30L + i, 1000.0, "COMPLETED",
                    Timestamp.valueOf(LocalDateTime.now()),
                    Timestamp.valueOf(LocalDateTime.now()));
        }
    }
}