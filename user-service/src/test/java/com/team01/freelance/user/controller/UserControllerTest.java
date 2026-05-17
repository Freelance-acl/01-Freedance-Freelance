package com.team01.freelance.user.controller;

import com.team01.freelance.user.dto.UserContractSummaryDTO;
import com.team01.freelance.user.dto.UserProfileDTO;
import com.team01.freelance.user.exception.GlobalExceptionHandler;
import com.team01.freelance.user.model.User;
import com.team01.freelance.user.model.UserRole;
import com.team01.freelance.user.model.UserSkill;
import com.team01.freelance.user.model.UserStatus;
import com.team01.freelance.user.service.UserService;
import com.team01.freelance.user.service.UserSkillService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import jakarta.persistence.EntityNotFoundException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest {

    private MockMvc mockMvc;
    private UserService userService;
    private UserSkillService userSkillService;

    @BeforeEach
    void setUp() {
        UserController controller = new UserController();
        userService = mock(UserService.class);
        userSkillService = mock(UserSkillService.class);
        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "userSkillService", userSkillService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getAllReturnsOk() throws Exception {
        when(userService.getAllUsers()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getByIdReturnsOk() throws Exception {
        User user = new User();
        when(userService.getUserById(1L)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/users/{id}", 1L))
                .andExpect(status().isOk());
    }

    @Test
    void getByIdReturnsNotFound() throws Exception {
        when(userService.getUserById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/{id}", 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void createReturnsOk() throws Exception {
        User user = new User();
        when(userService.createUser(any(User.class))).thenReturn(user);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateReturnsOk() throws Exception {
        User user = new User();
        when(userService.updateUser(eq(1L), any(User.class))).thenReturn(user);

        mockMvc.perform(put("/api/users/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateReturnsBadRequest() throws Exception {
        when(userService.updateUser(eq(1L), any(User.class)))
                .thenThrow(new IllegalArgumentException("Invalid email"));

        mockMvc.perform(put("/api/users/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateReturnsNotFound() throws Exception {
        when(userService.updateUser(eq(999L), any(User.class)))
                .thenThrow(new EntityNotFoundException("User not found with id: 999"));

        mockMvc.perform(put("/api/users/{id}", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deactivateReturnsOk() throws Exception {
        User user = new User();
        user.setStatus(UserStatus.DEACTIVATED);
        when(userService.deactivateUser(1L)).thenReturn(user);

        mockMvc.perform(put("/api/users/{id}/deactivate", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEACTIVATED"));
    }

    @Test
    void deactivateReturnsBadRequestWhenActiveContractExists() throws Exception {
        when(userService.deactivateUser(1L))
                .thenThrow(new IllegalStateException("Cannot deactivate user with active contracts"));

        mockMvc.perform(put("/api/users/{id}/deactivate", 1L))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchByPreferenceReturnsMatchingUsers() throws Exception {
        User first = new User();
        first.setName("Arabic User 1");
        User second = new User();
        second.setName("Arabic User 2");
        when(userService.findUsersByPreference("language", "ar")).thenReturn(List.of(first, second));

        mockMvc.perform(get("/api/users/preferences/search")
                        .param("key", "language")
                        .param("value", "ar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void searchByPreferenceReturnsEmptyListWhenNoUsersMatch() throws Exception {
        when(userService.findUsersByPreference("language", "fr")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/users/preferences/search")
                        .param("key", "language")
                        .param("value", "fr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void searchByPreferenceReturnsBadRequestWhenKeyIsBlank() throws Exception {
        when(userService.findUsersByPreference("", "ar"))
                .thenThrow(new IllegalArgumentException("Preference key and value must not be blank"));

        mockMvc.perform(get("/api/users/preferences/search")
                        .param("key", "")
                        .param("value", "ar"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteByIdReturnsNoContent() throws Exception {
        when(userService.deleteUserById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/users/{id}", 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteByIdReturnsNotFound() throws Exception {
        when(userService.deleteUserById(999L)).thenReturn(false);

        mockMvc.perform(delete("/api/users/{id}", 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void getByIdReturnsOkAndDoesNotExposePassword() throws Exception {
        User user = new User();
        user.setName("John Doe");
        user.setPassword("secret123");
        when(userService.getUserById(1L)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/users/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void deleteAllReturnsNoContent() throws Exception {
        doNothing().when(userService).deleteAllUsers();

        mockMvc.perform(delete("/api/users/all"))
                .andExpect(status().isNoContent());
    }

    // -----------------------------------------------------------------------
    // [S1-F1] Search Users
    // -----------------------------------------------------------------------

    @Test
    void searchUsers_returnsOkWithResults() throws Exception {
        User ahmed = new User();
        ahmed.setName("Ahmed");
        ahmed.setRole(UserRole.FREELANCER);

        when(userService.searchUsers(eq("Ahmed"), isNull(), isNull()))
                .thenReturn(List.of(ahmed));

        mockMvc.perform(get("/api/users/search").param("name", "Ahmed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Ahmed"));
    }

    @Test
    void searchUsers_noMatches_returnsEmptyList() throws Exception {
        when(userService.searchUsers(eq("xyz"), isNull(), isNull()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/users/search").param("name", "xyz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void searchUsers_invalidRole_returns400WithValidValues() throws Exception {
        mockMvc.perform(get("/api/users/search").param("role", "INVALID_ROLE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("INVALID_ROLE")))
                .andExpect(jsonPath("$.message", containsString("role")))
                .andExpect(jsonPath("$.message", containsString("FREELANCER")))
                .andExpect(jsonPath("$.message", containsString("CLIENT")));
    }

    // -----------------------------------------------------------------------
    // [S1-F2] Update User Preferences
    // -----------------------------------------------------------------------

    @Test
    void updatePreferences_returnsOkWithMergedPreferences() throws Exception {
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        Map<String, Object> merged = new LinkedHashMap<>();
        merged.put("language", "en");
        merged.put("timezone", "Africa/Cairo");
        merged.put("hourlyRate", 45);
        user.setPreferences(merged);

        when(userService.updatePreferences(eq(userId), any())).thenReturn(user);

        mockMvc.perform(put("/api/users/{id}/preferences", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"timezone":"Africa/Cairo","hourlyRate":45}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferences.language").value("en"))
                .andExpect(jsonPath("$.preferences.timezone").value("Africa/Cairo"))
                .andExpect(jsonPath("$.preferences.hourlyRate").value(45));
    }

    @Test
    void updatePreferences_userNotFound_returns404() throws Exception {
        when(userService.updatePreferences(eq(999L), any()))
                .thenThrow(new EntityNotFoundException("User not found with id: 999"));

        mockMvc.perform(put("/api/users/{id}/preferences", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"timezone\":\"UTC\"}"))
                .andExpect(status().isNotFound());
    }

    // -----------------------------------------------------------------------
    // [S1-F3] User Contract Summary
    // -----------------------------------------------------------------------

    @Test
    void getUserContractSummary_returnsOkWithMetrics() throws Exception {
        Long userId = 1L;
        UserContractSummaryDTO dto = new UserContractSummaryDTO(
                userId, "Freelancer", 5L, 3L, 1L, 3000.0, 1000.0);

        when(userService.getUserContractSummary(userId)).thenReturn(dto);

        mockMvc.perform(get("/api/users/{id}/contract-summary", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.name").value("Freelancer"))
                .andExpect(jsonPath("$.totalContracts").value(5))
                .andExpect(jsonPath("$.completedContracts").value(3))
                .andExpect(jsonPath("$.terminatedContracts").value(1))
                .andExpect(jsonPath("$.totalEarnings").value(3000.0))
                .andExpect(jsonPath("$.averageContractValue").value(1000.0));
    }

    @Test
    void getUserContractSummary_userNotFound_returns404() throws Exception {
        when(userService.getUserContractSummary(999L))
                .thenThrow(new EntityNotFoundException("User not found with id: 999"));

        mockMvc.perform(get("/api/users/{id}/contract-summary", 999L))
                .andExpect(status().isNotFound());
    }

    // -----------------------------------------------------------------------
    // [S1-F7] Set Primary Skill
    // -----------------------------------------------------------------------

    @Test
    void setPrimarySkill_returnsOkWithUserAndSkills() throws Exception {
        User user = new User();
        user.setId(1L);
        UserSkill skill = new UserSkill();
        skill.setId(2L);
        skill.setSkillName("Spring");
        skill.setIsPrimary(true);
        user.setUserSkills(List.of(skill));

        when(userSkillService.setPrimarySkill(1L, 2L)).thenReturn(user);

        mockMvc.perform(put("/api/users/{userId}/skills/{skillId}/primary", 1L, 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userSkills[0].skillName").value("Spring"))
                .andExpect(jsonPath("$.userSkills[0].isPrimary").value(true));
    }

    @Test
    void setPrimarySkill_returnsBadRequestWhenSkillBelongsToOtherUser() throws Exception {
        when(userSkillService.setPrimarySkill(1L, 2L))
                .thenThrow(new IllegalArgumentException("Skill does not belong to this user"));

        mockMvc.perform(put("/api/users/{userId}/skills/{skillId}/primary", 1L, 2L))
                .andExpect(status().isBadRequest());
    }

    @Test
    void setPrimarySkill_returnsNotFound() throws Exception {
        when(userSkillService.setPrimarySkill(1L, 2L))
                .thenThrow(new EntityNotFoundException("User not found with id: 1"));

        mockMvc.perform(put("/api/users/{userId}/skills/{skillId}/primary", 1L, 2L))
                .andExpect(status().isNotFound());
    }

    // -----------------------------------------------------------------------
    // User Profile
    // -----------------------------------------------------------------------

    @Test
    void getUserProfile_returnsOkWithProfile() throws Exception {
        Long userId = 1L;
        UserProfileDTO dto = new UserProfileDTO(
                userId, "Omar Taha", "omar@example.com", "01000000000",
                Map.of("language", "en"), Collections.emptyList(), 0);

        when(userService.getUserProfile(userId)).thenReturn(dto);

        mockMvc.perform(get("/api/users/{id}/profile", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.name").value("Omar Taha"))
                .andExpect(jsonPath("$.email").value("omar@example.com"))
                .andExpect(jsonPath("$.totalSkills").value(0));
    }

    @Test
    void getUserProfile_userNotFound_returns404() throws Exception {
        when(userService.getUserProfile(999L))
                .thenThrow(new EntityNotFoundException("User not found with id: 999"));

        mockMvc.perform(get("/api/users/{id}/profile", 999L))
                .andExpect(status().isNotFound());
    }

    // -----------------------------------------------------------------------
    // Users by language preference
    // -----------------------------------------------------------------------

    @Test
    void getUsersByLanguage_returnsOkWithUsers() throws Exception {
        User user = new User();
        user.setName("Ahmed");
        when(userService.findUsersByLanguageAndMinimumCompletedContracts("en", 2L))
                .thenReturn(List.of(user));

        mockMvc.perform(get("/api/users/preferences/language")
                        .param("lang", "en")
                        .param("minContracts", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Ahmed"));
    }

    @Test
    void getUsersByLanguage_invalidLanguage_returns400() throws Exception {
        when(userService.findUsersByLanguageAndMinimumCompletedContracts("   ", 0L))
                .thenThrow(new IllegalArgumentException("Language must not be blank"));

        mockMvc.perform(get("/api/users/preferences/language")
                        .param("lang", "   "))
                .andExpect(status().isBadRequest());
    }
}
