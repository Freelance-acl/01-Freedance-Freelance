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
        if (!userSkillRepository.existsById(id)) {
            return Optional.empty();
        }
        userSkill.setId(id);
        return Optional.of(userSkillRepository.save(userSkill));
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
