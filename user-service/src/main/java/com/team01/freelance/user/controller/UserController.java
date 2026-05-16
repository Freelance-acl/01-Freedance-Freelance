package com.team01.freelance.user.controller;

import com.team01.freelance.user.model.User;
import com.team01.freelance.user.service.UserService;
import com.team01.freelance.user.service.UserSkillService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.team01.freelance.user.model.UserRole;
import java.util.Map;
import java.util.List;
import com.team01.freelance.user.dto.UserContractSummaryDTO;
import com.team01.freelance.user.dto.UserProfileDTO;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserSkillService userSkillService;

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        return ResponseEntity.ok(userService.createUser(user));
    }

    /**
     * Updates a user by ID.
     *
     * @param id the user ID
     * @param user the update payload
     * @return 200 with updated user, or 404 if not found
     */
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        try {
            return ResponseEntity.ok(userService.updateUser(id, user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUserById(@PathVariable Long id) {
        if (userService.deleteUserById(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/all")
    public ResponseEntity<Void> deleteAllUsers() {
        userService.deleteAllUsers();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<User>> searchUsers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) UserRole role) {
        return ResponseEntity.ok(userService.searchUsers(name, email, role));
    }

    @GetMapping("/preferences/language")
    public ResponseEntity<List<User>> getUsersByLanguageAndMinimumCompletedContracts(
            @RequestParam String lang,
            @RequestParam(defaultValue = "0") Long minContracts) {
        try {
            return ResponseEntity.ok(userService.findUsersByLanguageAndMinimumCompletedContracts(lang, minContracts));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/preferences")
    public ResponseEntity<User> updatePreferences(
            @PathVariable Long id,
            @RequestBody Map<String, Object> preferences) {
        try {
            return ResponseEntity.ok(userService.updatePreferences(id, preferences));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/profile")
    public ResponseEntity<UserProfileDTO> getUserProfile(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(userService.getUserProfile(id));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/contract-summary")
    public ResponseEntity<UserContractSummaryDTO> getUserContractSummary(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(userService.getUserContractSummary(id));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Sets a user skill as the sole primary skill for that user.
     *
     * @param userId the user ID
     * @param skillId the user-skill ID
     * @return 200 with user and skills, 400 if skill belongs to another user, or 404 if not found
     */
    @PutMapping("/{userId}/skills/{skillId}/primary")
    public ResponseEntity<User> setPrimarySkill(
            @PathVariable Long userId,
            @PathVariable Long skillId) {
        try {
            return ResponseEntity.ok(userSkillService.setPrimarySkill(userId, skillId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
