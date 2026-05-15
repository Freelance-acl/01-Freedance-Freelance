package com.team01.freelance.user.controller;

import com.team01.freelance.user.exception.GlobalExceptionHandler;
import com.team01.freelance.user.model.User;
import com.team01.freelance.user.model.UserRole;
import com.team01.freelance.user.service.UserService;
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

    @BeforeEach
    void setUp() {
        UserController controller = new UserController();
        userService = mock(UserService.class);
        ReflectionTestUtils.setField(controller, "userService", userService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getAllReturnsOk() throws Exception {
        when(userService.searchUsers(isNull(), isNull(), isNull()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk());
    }

    @Test
    void getByIdReturnsOk() throws Exception {
        User user = new User();
        when(userService.getUserById(1L)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/users/{id}", 1L))
                .andExpect(status().isOk());
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
    void deleteByIdReturnsNoContent() throws Exception {
        when(userService.deleteUserById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/users/{id}", 1L))
                .andExpect(status().isNoContent());
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
}
