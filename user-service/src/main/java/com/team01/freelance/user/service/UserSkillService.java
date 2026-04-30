package com.team01.freelance.user.service;

import com.team01.freelance.user.model.UserSkill;
import com.team01.freelance.user.repository.UserRepository;
import com.team01.freelance.user.repository.UserSkillRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
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
        if (userSkill.getUser() != null && userSkill.getUser().getId() != null) {
            userSkill.setUser(userRepository.findById(userSkill.getUser().getId())
                    .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userSkill.getUser().getId())));
        }
        return userSkillRepository.save(userSkill);
    }

    /**
     * Updates an existing user skill with non-null fields from the provided user skill object.
     * Validates the existence of the associated user if provided.
     *
     * @param id The ID of the user skill to update
     * @param userSkill The user skill object containing updated fields
     * @return The updated user skill
     * @throws EntityNotFoundException if the user skill or associated user is not found
     */
    public UserSkill updateUserSkill(Long id, UserSkill userSkill) {
        return userSkillRepository.findById(id).map(existing -> {
                if (userSkill.getSkillName() != null) existing.setSkillName(userSkill.getSkillName());
                if (userSkill.getCategory() != null) existing.setCategory(userSkill.getCategory());
                if (userSkill.getYearsOfExperience() != null) existing.setYearsOfExperience(userSkill.getYearsOfExperience());
                if (userSkill.getProficiencyLevel() != null) existing.setProficiencyLevel(userSkill.getProficiencyLevel());
                if (userSkill.getIsPrimary() != null) existing.setIsPrimary(userSkill.getIsPrimary());
                if (userSkill.getMetadata() != null) existing.setMetadata(userSkill.getMetadata());
                if (userSkill.getUser() != null && userSkill.getUser().getId() != null) {
                    existing.setUser(userRepository.findById(userSkill.getUser().getId())
                            .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userSkill.getUser().getId())));
                }
            return userSkillRepository.save(existing);
        }).orElseThrow(() -> new EntityNotFoundException("User Skill not found with id: " + id));
    }

    public boolean deleteUserSkillById(Long id) {
        if (!userSkillRepository.existsById(id)) {
            return false;
        }
        userSkillRepository.deleteById(id);
        return true;
    }

    public void deleteAllUserSkills() {
        userSkillRepository.deleteAll();
    }
}
