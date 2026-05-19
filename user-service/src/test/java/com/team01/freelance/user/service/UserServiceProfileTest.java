package com.team01.freelance.user.service;

import com.team01.freelance.user.dto.UserProfileDTO;
import com.team01.freelance.user.model.ProficiencyLevel;
import com.team01.freelance.user.model.User;
import com.team01.freelance.user.model.UserSkill;
import com.team01.freelance.user.repository.UserRepository;
import com.team01.freelance.user.repository.UserSkillRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceProfileTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSkillRepository userSkillRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void getUserProfile_existingUserWithSkills_returnsProfileWithSkills() {
        Long userId = 1L;

        User user = new User();
        user.setId(userId);
        user.setName("Omar Taha");
        user.setEmail("omar@example.com");
        user.setPhone("01000000000");
        user.setPreferences(Map.of("language", "en"));

        UserSkill skill = new UserSkill();
        skill.setSkillName("Java");
        skill.setCategory("Backend");
        skill.setYearsOfExperience(2);
        skill.setProficiencyLevel(ProficiencyLevel.INTERMEDIATE);
        skill.setIsPrimary(true);
        skill.setMetadata(Map.of("verified", true));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userSkillRepository.findByUserId(userId)).thenReturn(List.of(skill));

        UserProfileDTO result = userService.getUserProfile(userId);

        assertEquals(userId, result.getUserId());
        assertEquals("Omar Taha", result.getName());
        assertEquals("omar@example.com", result.getEmail());
        assertEquals("01000000000", result.getPhone());
        assertEquals("en", result.getPreferences().get("language"));

        assertEquals(1, result.getTotalSkills());
        assertEquals(1, result.getSkills().size());
        assertEquals("Java", result.getSkills().get(0).getSkillName());
        assertEquals("Backend", result.getSkills().get(0).getCategory());
        assertEquals(2, result.getSkills().get(0).getYearsOfExperience());
        assertEquals(ProficiencyLevel.INTERMEDIATE, result.getSkills().get(0).getProficiencyLevel());
        assertEquals(true, result.getSkills().get(0).getIsPrimary());
        assertEquals(true, result.getSkills().get(0).getMetadata().get("verified"));

        verify(userRepository).findById(userId);
        verify(userSkillRepository).findByUserId(userId);
    }

    @Test
    void getUserProfile_existingUserWithoutSkills_returnsEmptySkillsAndZeroTotal() {
        Long userId = 2L;

        User user = new User();
        user.setId(userId);
        user.setName("No Skills User");
        user.setEmail("noskills@example.com");
        user.setPhone("01111111111");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userSkillRepository.findByUserId(userId)).thenReturn(List.of());

        UserProfileDTO result = userService.getUserProfile(userId);

        assertEquals(userId, result.getUserId());
        assertEquals("No Skills User", result.getName());
        assertEquals("noskills@example.com", result.getEmail());
        assertEquals("01111111111", result.getPhone());
        assertEquals(0, result.getTotalSkills());
        assertEquals(0, result.getSkills().size());

        verify(userRepository).findById(userId);
        verify(userSkillRepository).findByUserId(userId);
    }

    @Test
    void getUserProfile_userNotFound_throwsEntityNotFoundException() {
        Long userId = 999L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> userService.getUserProfile(userId));

        verify(userRepository).findById(userId);
        verify(userSkillRepository, never()).findByUserId(userId);
    }
}