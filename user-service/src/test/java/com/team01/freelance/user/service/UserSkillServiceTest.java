package com.team01.freelance.user.service;

import com.team01.freelance.user.model.ProficiencyLevel;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserSkillServiceTest {

    @Mock
    private UserSkillRepository userSkillRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserSkillService userSkillService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void updateUserSkill_ShouldMergeNonNullFields() {
        // Arrange
        Long id = 1L;
        UserSkill existing = new UserSkill();
        existing.setId(id);
        existing.setSkillName("Java");
        existing.setCategory("Programming");
        existing.setYearsOfExperience(5);
        existing.setProficiencyLevel(ProficiencyLevel.EXPERT);
        existing.setIsPrimary(true);

        UserSkill incoming = new UserSkill();
        incoming.setYearsOfExperience(6);
        incoming.setIsPrimary(false);
        // skillName, category, proficiencyLevel are null in incoming

        when(userSkillRepository.findById(id)).thenReturn(Optional.of(existing));
        when(userSkillRepository.save(any(UserSkill.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        UserSkill updated = userSkillService.updateUserSkill(id, incoming);

        // Assert
        assertNotNull(updated);
        assertEquals(id, updated.getId());
        assertEquals("Java", updated.getSkillName()); // Preserved
        assertEquals("Programming", updated.getCategory()); // Preserved
        assertEquals(6, updated.getYearsOfExperience()); // Updated
        assertEquals(ProficiencyLevel.EXPERT, updated.getProficiencyLevel()); // Preserved
        assertFalse(updated.getIsPrimary()); // Updated
        
        verify(userSkillRepository).findById(id);
        verify(userSkillRepository).save(updated);
    }

    @Test
    void updateUserSkill_ThrowExceptionIfNotFound() {
        // Arrange
        Long id = 1L;
        UserSkill incoming = new UserSkill();
        when(userSkillRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> userSkillService.updateUserSkill(id, incoming));
        verify(userSkillRepository).findById(id);
        verify(userSkillRepository, never()).save(any());
    }

    @Test
    void createUserSkill_ShouldValidateUser() {
        // Arrange
        UserSkill skill = new UserSkill();
        User user = new User();
        user.setId(10L);
        skill.setUser(user);

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(userSkillRepository.save(skill)).thenReturn(skill);

        // Act
        UserSkill result = userSkillService.createUserSkill(skill);

        // Assert
        assertNotNull(result);
        verify(userRepository).findById(10L);
        verify(userSkillRepository).save(skill);
    }

    @Test
    void createUserSkill_ShouldThrowIfUserNotFound() {
        // Arrange
        UserSkill skill = new UserSkill();
        User user = new User();
        user.setId(10L);
        skill.setUser(user);

        when(userRepository.findById(10L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> userSkillService.createUserSkill(skill));
        verify(userRepository).findById(10L);
        verify(userSkillRepository, never()).save(any());
    }
}
