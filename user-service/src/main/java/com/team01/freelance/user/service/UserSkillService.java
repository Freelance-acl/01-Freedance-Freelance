package com.team01.freelance.user.service;

import com.team01.freelance.user.model.UserSkill;
import com.team01.freelance.user.repository.UserSkillRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserSkillService {

    @Autowired
    private UserSkillRepository userSkillRepository;

    public List<UserSkill> getAllUserSkills() {
        return userSkillRepository.findAll();
    }

    public Optional<UserSkill> getUserSkillById(Long id) {
        return userSkillRepository.findById(id);
    }

    public UserSkill createUserSkill(UserSkill userSkill) {
        return userSkillRepository.save(userSkill);
    }

    public Optional<UserSkill> updateUserSkill(Long id, UserSkill userSkill) {
        return userSkillRepository.findById(id).map(existing -> {
            if (userSkill.getSkillName() != null) {
                existing.setSkillName(userSkill.getSkillName());
            }
            if (userSkill.getCategory() != null) {
                existing.setCategory(userSkill.getCategory());
            }
            if (userSkill.getYearsOfExperience() != null) {
                existing.setYearsOfExperience(userSkill.getYearsOfExperience());
            }
            if (userSkill.getProficiencyLevel() != null) {
                existing.setProficiencyLevel(userSkill.getProficiencyLevel());
            }
            if (userSkill.getIsPrimary() != null) {
                existing.setIsPrimary(userSkill.getIsPrimary());
            }
            if (userSkill.getMetadata() != null) {
                existing.setMetadata(userSkill.getMetadata());
            }
            if (userSkill.getUser() != null) {
                existing.setUser(userSkill.getUser());
            }
            return userSkillRepository.save(existing);
        });
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
