package com.team01.freelance.user.controller;

import com.team01.freelance.user.model.ProficiencyLevel;
import com.team01.freelance.user.model.User;
import com.team01.freelance.user.model.UserRole;
import com.team01.freelance.user.model.UserSkill;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [S1-F8] Integration tests for {@code GET /api/users/{id}/profile}.
 */
@Transactional
class UserProfileIntegrationTest extends AbstractIntegrationTest {

    private static final String PROFILE_URL = "/api/users/{id}/profile";

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
    void getUserProfile_withSkills_returnsFullProfile() throws Exception {
        long suffix = System.nanoTime();
        User user = new User();
        user.setName("Omar Taha");
        user.setEmail("omar-" + suffix + "@test.dev");
        user.setPassword("secret");
        user.setPhone("+30111" + (suffix % 1_000_000L));
        user.setRole(UserRole.FREELANCER);
        user.setStatus(UserStatus.ACTIVE);
        user.setPreferences(Map.of("language", "en"));

        UserSkill primary = buildSkill("Java", true);
        UserSkill secondary = buildSkill("Spring", false);
        List<UserSkill> skills = new ArrayList<>();
        skills.add(primary);
        skills.add(secondary);
        user.setUserSkills(skills);
        user = userRepository.save(user);

        mockMvc.perform(get(PROFILE_URL, user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getId().intValue()))
                .andExpect(jsonPath("$.name").value("Omar Taha"))
                .andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.preferences.language").value("en"))
                .andExpect(jsonPath("$.totalSkills").value(2))
                .andExpect(jsonPath("$.skills[?(@.skillName=='Java')].isPrimary", contains(true)))
                .andExpect(jsonPath("$.skills[?(@.skillName=='Spring')].isPrimary", contains(false)));
    }

    @Test
    void getUserProfile_noSkills_returnsEmptySkills() throws Exception {
        User user = new User();
        user.setName("No Skills");
        user.setEmail("noskills-" + System.nanoTime() + "@test.dev");
        user.setPassword("secret");
        user.setPhone("+30222");
        user.setRole(UserRole.CLIENT);
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);

        mockMvc.perform(get(PROFILE_URL, user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSkills").value(0))
                .andExpect(jsonPath("$.skills").isEmpty());
    }

    @Test
    void getUserProfile_unknownUser_returns404() throws Exception {
        mockMvc.perform(get(PROFILE_URL, 999_999L))
                .andExpect(status().isNotFound());
    }

    private static UserSkill buildSkill(String name, boolean primary) {
        UserSkill skill = new UserSkill();
        skill.setSkillName(name);
        skill.setCategory("DEVELOPMENT");
        skill.setYearsOfExperience(3);
        skill.setProficiencyLevel(ProficiencyLevel.INTERMEDIATE);
        skill.setIsPrimary(primary);
        return skill;
    }
}
