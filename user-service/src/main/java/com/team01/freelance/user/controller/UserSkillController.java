package com.team01.freelance.user.controller;

import com.team01.freelance.user.model.UserSkill;
import com.team01.freelance.user.service.UserSkillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user-skills")
public class UserSkillController {

    @Autowired
    private UserSkillService userSkillService;

    @GetMapping
    public ResponseEntity<List<UserSkill>> getAllUserSkills() {
        return ResponseEntity.ok(userSkillService.getAllUserSkills());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserSkill> getUserSkillById(@PathVariable Long id) {
        return userSkillService.getUserSkillById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<UserSkill> createUserSkill(@RequestBody UserSkill userSkill) {
        return ResponseEntity.ok(userSkillService.createUserSkill(userSkill));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserSkill> updateUserSkill(@PathVariable Long id, @RequestBody UserSkill userSkill) {
        return userSkillService.updateUserSkill(id, userSkill)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUserSkillById(@PathVariable Long id) {
        if (userSkillService.deleteUserSkillById(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/all")
    public ResponseEntity<Void> deleteAllUserSkills() {
        userSkillService.deleteAllUserSkills();
        return ResponseEntity.noContent().build();
    }
}
