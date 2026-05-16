package com.team01.freelance.user.service;

import com.team01.freelance.user.model.UserSkill;
import com.team01.freelance.user.repository.UserRepository;
import com.team01.freelance.user.repository.UserSkillRepository;
import jakarta.persistence.EntityNotFoundException;
import com.team01.freelance.user.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class UserSkillService {

    @Autowired
    private UserSkillRepository userSkillRepository;

    @Autowired
    private UserRepository userRepository;

    public List<UserSkill> getAllUserSkills() {
        return userSkillRepository.findAll();
    }

    public Optional<UserSkill> getUserSkillById(Long id) {
        return userSkillRepository.findById(id);
    }

    public UserSkill createUserSkill(UserSkill userSkill) {
        if (userSkill.getUser() == null || userSkill.getUser().getId() == null) {
            throw new IllegalArgumentException("User ID is required to create a UserSkill");
        }

        userSkill.setUser(userRepository.findById(userSkill.getUser().getId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userSkill.getUser().getId())));

        return userSkillRepository.save(userSkill);
    }

    /**
     * Updates editable fields on an existing user skill.
     * The associated user cannot be changed after creation.
     *
     * @param id The ID of the user skill to update
     * @param userSkill The user skill object containing updated fields
     * @return The updated user skill
     * @throws EntityNotFoundException if the user skill is not found
     */
    public UserSkill updateUserSkill(Long id, UserSkill userSkill) {
        return userSkillRepository.findById(id).map(existing -> {
                if (userSkill.getSkillName() != null) existing.setSkillName(userSkill.getSkillName());
                if (userSkill.getCategory() != null) existing.setCategory(userSkill.getCategory());
                if (userSkill.getYearsOfExperience() != null) existing.setYearsOfExperience(userSkill.getYearsOfExperience());
                if (userSkill.getProficiencyLevel() != null) existing.setProficiencyLevel(userSkill.getProficiencyLevel());
                if (userSkill.getIsPrimary() != null) existing.setIsPrimary(userSkill.getIsPrimary());
                if (userSkill.getMetadata() != null) existing.setMetadata(userSkill.getMetadata());
                if (userSkill.getCreatedAt() != null) existing.setCreatedAt(userSkill.getCreatedAt());
            return userSkillRepository.save(existing);
        }).orElseThrow(() -> new EntityNotFoundException("User Skill not found with id: " + id));
    }

     public boolean deleteUserSkillById(Long id) {

        return userSkillRepository.findById(id).map(existing -> {
            userSkillRepository.delete(existing);
            return true;
        }).orElse(false);
    }

    public void deleteAllUserSkills() {
        userSkillRepository.deleteAll();
    }

    /**
     * Sets the given skill as the user's sole primary skill (transactional, via user cascade).
     *
     * @param userId the user ID
     * @param skillId the user-skill ID to mark primary
     * @return the user with updated skills
     * @throws EntityNotFoundException if the user or skill is not found
     * @throws IllegalArgumentException if the skill does not belong to the user
     */
    @Transactional
    public User setPrimarySkill(Long userId, Long skillId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        UserSkill target = userSkillRepository.findById(skillId)
                .orElseThrow(() -> new EntityNotFoundException("User Skill not found with id: " + skillId));

        if (target.getUser() == null || !Objects.equals(userId, target.getUser().getId())) {
            throw new IllegalArgumentException("Skill does not belong to this user");
        }

        List<UserSkill> skills = user.getUserSkills();
        if (skills == null || skills.isEmpty()) {
            user.setUserSkills(new ArrayList<>(userSkillRepository.findByUser_Id(userId)));
            skills = user.getUserSkills();
        }

        boolean skillFoundForUser = false;
        for (UserSkill skill : skills) {
            boolean isTarget = Objects.equals(skill.getId(), skillId);
            skill.setIsPrimary(isTarget);
            if (isTarget) {
                skillFoundForUser = true;
            }
        }
        if (!skillFoundForUser) {
            throw new EntityNotFoundException("User Skill not found with id: " + skillId);
        }

        return userRepository.save(user);
    }
}
