package com.team01.freelance.user.controller;
import com.team01.freelance.user.model.ProficiencyLevel;
import com.team01.freelance.user.model.User;
import com.team01.freelance.user.model.UserRole;
import com.team01.freelance.user.model.UserSkill;
import com.team01.freelance.user.model.UserStatus;
import com.team01.freelance.user.repository.UserRepository;
import com.team01.freelance.user.repository.UserSkillRepository;
import com.team01.freelance.user.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.test.context.support.WithMockUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [S1-F7] Integration tests for {@code PUT /api/users/{userId}/skills/{skillId}/primary}.
 */
@Transactional
@WithMockUser(roles = "ADMIN")
class SetPrimarySkillIntegrationTest extends AbstractIntegrationTest {

    private static final String PRIMARY_URL = "/api/users/{userId}/skills/{skillId}/primary";

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSkillRepository userSkillRepository;

    private User user;
    private UserSkill skill1;
    private UserSkill skill2;
    private UserSkill skill3;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(webApplicationContext);

        long suffix = System.nanoTime();
        user = new User();
        user.setName("Skill User");
        user.setEmail("skills-" + suffix + "@test.dev");
        user.setPassword("secret");
        user.setPhone("+3000" + (suffix % 1_000_000_000L));
        user.setRole(UserRole.FREELANCER);
        user.setStatus(UserStatus.ACTIVE);

        skill1 = buildSkill("Java", "Backend");
        skill2 = buildSkill("Spring", "Backend");
        skill3 = buildSkill("PostgreSQL", "Database");

        List<UserSkill> skills = new ArrayList<>();
        skills.add(skill1);
        skills.add(skill2);
        skills.add(skill3);
        user.setUserSkills(skills);
        user = userRepository.save(user);

        skill1 = user.getUserSkills().get(0);
        skill2 = user.getUserSkills().get(1);
        skill3 = user.getUserSkills().get(2);
    }

    /**
     * Spec scenarios (a)+(b): set skill #2 primary, then skill #3; only one primary at a time.
     */
    @Test
    void setPrimarySkill_switchesPrimaryBetweenUserSkills() throws Exception {
        mockMvc.perform(put(PRIMARY_URL, user.getId(), skill2.getId()))
                .andExpect(status().isOk());

        assertTrue(findSkill(skill2.getId()).getIsPrimary());
        assertFalse(findSkill(skill1.getId()).getIsPrimary());
        assertFalse(findSkill(skill3.getId()).getIsPrimary());

        mockMvc.perform(put(PRIMARY_URL, user.getId(), skill3.getId()))
                .andExpect(status().isOk());

        assertTrue(findSkill(skill3.getId()).getIsPrimary());
        assertFalse(findSkill(skill2.getId()).getIsPrimary());
        assertFalse(findSkill(skill1.getId()).getIsPrimary());
    }

    /** Spec scenario (c): unknown user → 404. */
    @Test
    void setPrimarySkill_unknownUser_returns404() throws Exception {
        mockMvc.perform(put(PRIMARY_URL, 999_999L, skill1.getId()))
                .andExpect(status().isNotFound());
    }

    /** Spec scenario (d): skill owned by another user → 400. */
    @Test
    void setPrimarySkill_skillBelongsToOtherUser_returns400() throws Exception {
        User other = new User();
        other.setName("Other");
        other.setEmail("other-" + System.nanoTime() + "@test.dev");
        other.setPassword("secret");
        other.setPhone("+4000" + (System.nanoTime() % 1_000_000_000L));
        other.setRole(UserRole.FREELANCER);
        other.setStatus(UserStatus.ACTIVE);
        UserSkill otherSkill = buildSkill("Go", "Backend");
        other.setUserSkills(List.of(otherSkill));
        other = userRepository.save(other);
        Long otherSkillId = other.getUserSkills().get(0).getId();

        mockMvc.perform(put(PRIMARY_URL, user.getId(), otherSkillId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void setPrimarySkill_unknownSkill_returns404() throws Exception {
        mockMvc.perform(put(PRIMARY_URL, user.getId(), 999_999L))
                .andExpect(status().isNotFound());
    }

    private UserSkill findSkill(Long skillId) {
        return userSkillRepository.findById(skillId).orElseThrow();
    }

    private static UserSkill buildSkill(String name, String category) {
        UserSkill skill = new UserSkill();
        skill.setSkillName(name);
        skill.setCategory(category);
        skill.setYearsOfExperience(3);
        skill.setProficiencyLevel(ProficiencyLevel.INTERMEDIATE);
        skill.setIsPrimary(false);
        return skill;
    }
}