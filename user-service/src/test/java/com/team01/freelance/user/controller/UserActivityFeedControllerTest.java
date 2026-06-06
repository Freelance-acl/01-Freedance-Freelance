package com.team01.freelance.user.controller;

import com.team01.freelance.user.dto.UserActivityFeedDTO;
import com.team01.freelance.user.service.UserService;
import com.team01.freelance.user.service.UserSkillService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class UserActivityFeedControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private UserSkillService userSkillService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        UserController controller = new UserController();
        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "userSkillService", userSkillService);
        mockMvc = standaloneSetup(controller).build();
    }

    @Test
    void getUserActivityFeed_delegatesToServiceWithPaginationAndAuthorizationHeader() throws Exception {
        UserActivityFeedDTO response = UserActivityFeedDTO.builder()
                .content(List.of())
                .page(1)
                .size(5)
                .totalElements(0)
                .totalPages(0)
                .build();

        when(userService.getUserActivityFeed(10L, 1, 5, "Bearer test-token"))
                .thenReturn(response);

        mockMvc.perform(get("/api/users/{id}/activity", 10L)
                        .param("page", "1")
                        .param("size", "5")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));

        verify(userService).getUserActivityFeed(10L, 1, 5, "Bearer test-token");
    }
}