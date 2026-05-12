package com.team01.freelance.user.controller;

import com.team01.freelance.user.model.UserSkill;
import com.team01.freelance.user.service.UserSkillService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserSkillControllerTest {

    private MockMvc mockMvc;
    private UserSkillService userSkillService;

    @BeforeEach
    void setUp() {
        UserSkillController controller = new UserSkillController();
        userSkillService = mock(UserSkillService.class);
        ReflectionTestUtils.setField(controller, "userSkillService", userSkillService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getAllReturnsOk() throws Exception {
        when(userSkillService.getAllUserSkills()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/user-skills"))
                .andExpect(status().isOk());
    }

    @Test
    void getByIdReturnsOk() throws Exception {
        UserSkill userSkill = new UserSkill();
        when(userSkillService.getUserSkillById(1L)).thenReturn(Optional.of(userSkill));

        mockMvc.perform(get("/api/user-skills/{id}", 1L))
                .andExpect(status().isOk());
    }

    @Test
    void createReturnsOk() throws Exception {
        UserSkill userSkill = new UserSkill();
        when(userSkillService.createUserSkill(any(UserSkill.class))).thenReturn(userSkill);

        mockMvc.perform(post("/api/user-skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateReturnsOk() throws Exception {
        UserSkill userSkill = new UserSkill();
        when(userSkillService.updateUserSkill(eq(1L), any(UserSkill.class))).thenReturn(userSkill);

        mockMvc.perform(put("/api/user-skills/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteByIdReturnsNoContent() throws Exception {
        when(userSkillService.deleteUserSkillById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/user-skills/{id}", 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteAllReturnsNoContent() throws Exception {
        doNothing().when(userSkillService).deleteAllUserSkills();

        mockMvc.perform(delete("/api/user-skills/all"))
                .andExpect(status().isNoContent());
    }
}
