package com.team01.freelance.user.service;

import com.team01.freelance.user.model.User;
import com.team01.freelance.user.model.UserSkill;
import com.team01.freelance.user.repository.UserRepository;
import com.team01.freelance.user.repository.UserSkillRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSkillRepository userSkillRepository;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void setPrimarySkill_ShouldSetTargetSkillAsPrimaryAndUnsetOtherSkills() {
        Long userId = 1L;
        Long targetSkillId = 3L;

        User user = new User();
        user.setId(userId);

        UserSkill skill1 = new UserSkill();
        skill1.setId(1L);
        skill1.setUser(user);
        skill1.setIsPrimary(false);

        UserSkill skill2 = new UserSkill();
        skill2.setId(2L);
        skill2.setUser(user);
        skill2.setIsPrimary(true);

        UserSkill skill3 = new UserSkill();
        skill3.setId(targetSkillId);
        skill3.setUser(user);
        skill3.setIsPrimary(false);

        user.setUserSkills(List.of(skill1, skill2, skill3));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userSkillRepository.findById(targetSkillId)).thenReturn(Optional.of(skill3));
        when(userRepository.save(user)).thenReturn(user);

        User updatedUser = userService.setPrimarySkill(userId, targetSkillId);

        assertNotNull(updatedUser);
        assertFalse(skill1.getIsPrimary());
        assertFalse(skill2.getIsPrimary());
        assertTrue(skill3.getIsPrimary());

        verify(userRepository).findById(userId);
        verify(userSkillRepository).findById(targetSkillId);
        verify(userRepository).save(user);
    }

    @Test
    void setPrimarySkill_ShouldThrowEntityNotFoundException_WhenUserNotFound() {
        Long userId = 999L;
        Long skillId = 1L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userService.setPrimarySkill(userId, skillId));

        verify(userRepository).findById(userId);
        verify(userSkillRepository, never()).findById(anyLong());
        verify(userRepository, never()).save(any());
    }

    @Test
    void setPrimarySkill_ShouldThrowEntityNotFoundException_WhenSkillNotFound() {
        Long userId = 1L;
        Long skillId = 999L;

        User user = new User();
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userSkillRepository.findById(skillId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userService.setPrimarySkill(userId, skillId));

        verify(userRepository).findById(userId);
        verify(userSkillRepository).findById(skillId);
        verify(userRepository, never()).save(any());
    }

    @Test
    void setPrimarySkill_ShouldThrowIllegalArgumentException_WhenSkillBelongsToDifferentUser() {
        Long userId = 1L;
        Long otherUserId = 2L;
        Long skillId = 5L;

        User user = new User();
        user.setId(userId);

        User otherUser = new User();
        otherUser.setId(otherUserId);

        UserSkill skill = new UserSkill();
        skill.setId(skillId);
        skill.setUser(otherUser);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userSkillRepository.findById(skillId)).thenReturn(Optional.of(skill));

        assertThrows(IllegalArgumentException.class, () -> userService.setPrimarySkill(userId, skillId));

        verify(userRepository).findById(userId);
        verify(userSkillRepository).findById(skillId);
        verify(userRepository, never()).save(any());
    }
}