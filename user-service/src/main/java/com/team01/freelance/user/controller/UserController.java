package com.team01.freelance.user.controller;

import com.team01.freelance.user.dto.TopFreelancerDTO;
import com.team01.freelance.user.model.User;
import com.team01.freelance.user.service.UserService;
import com.team01.freelance.user.service.UserSkillService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

import java.time.LocalDate;
import java.util.List;
import com.team01.freelance.user.dto.UpdateUserRoleRequest;
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
    @PreAuthorize("hasRole('FREELANCER') or hasRole('CLIENT') or hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('FREELANCER') or hasRole('CLIENT') or hasRole('ADMIN')")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('FREELANCER') or hasRole('CLIENT') or hasRole('ADMIN')")
    public ResponseEntity<User> createUser(@RequestBody User user) {
        return ResponseEntity.ok(userService.createUser(user));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('FREELANCER') or hasRole('CLIENT') or hasRole('ADMIN')")
    public ResponseEntity<User> deactivateUser(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(userService.deactivateUser(id));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/preferences/search")
    @PreAuthorize("hasRole('FREELANCER') or hasRole('CLIENT') or hasRole('ADMIN')")
    public ResponseEntity<List<User>> searchByPreference(
            @RequestParam String key,
            @RequestParam String value) {
        try {
            return ResponseEntity.ok(userService.findUsersByPreference(key, value));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/reports/top-freelancers")
    @PreAuthorize("hasRole('FREELANCER') or hasRole('CLIENT') or hasRole('ADMIN')")
    public ResponseEntity<List<TopFreelancerDTO>> getTopFreelancers(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "10") Integer limit) {
        try {
            return ResponseEntity.ok(userService.getTopFreelancersByEarnings(startDate, endDate, limit));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Updates a user by ID.
     *
     * @param id the user ID
     * @param user the update payload
     * @return 200 with updated user, or 404 if not found
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('FREELANCER') or hasRole('CLIENT') or hasRole('ADMIN')")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        try {
            return ResponseEntity.ok(userService.updateUser(id, user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> updateUserRole(@PathVariable Long id, @RequestBody UpdateUserRoleRequest request) {
        try {
            return ResponseEntity.ok(userService.updateUserRole(id, request.role()));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('FREELANCER') or hasRole('CLIENT') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUserById(@PathVariable Long id) {
        if (userService.deleteUserById(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/all")
    @PreAuthorize("hasRole('FREELANCER') or hasRole('CLIENT') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAllUsers() {
        userService.deleteAllUsers();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('FREELANCER') or hasRole('CLIENT') or hasRole('ADMIN')")
    public ResponseEntity<List<User>> searchUsers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) UserRole role) {
        return ResponseEntity.ok(userService.searchUsers(name, email, role));
    }

    
    @PutMapping("/{id}/preferences")
    @PreAuthorize("hasRole('FREELANCER') or hasRole('CLIENT') or hasRole('ADMIN')")
    public ResponseEntity<User> updatePreferences(
        @PathVariable Long id,
        @RequestBody Map<String, Object> preferences) {
            try {
                return ResponseEntity.ok(userService.updatePreferences(id, preferences));
            } catch (EntityNotFoundException e) {
                return ResponseEntity.notFound().build();
            }
        }
        
    
        @GetMapping("/{id}/contract-summary")
        @PreAuthorize("hasRole('FREELANCER') or hasRole('CLIENT') or hasRole('ADMIN')")
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
   @PreAuthorize("hasRole('FREELANCER') or hasRole('CLIENT') or hasRole('ADMIN')")
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
        
        @GetMapping("/{id}/profile")
        @PreAuthorize("hasRole('FREELANCER') or hasRole('CLIENT') or hasRole('ADMIN')")
        public ResponseEntity<UserProfileDTO> getUserProfile(@PathVariable Long id) {
            try {
                return ResponseEntity.ok(userService.getUserProfile(id));
            } catch (EntityNotFoundException e) {
                return ResponseEntity.notFound().build();
            }
        }
        @GetMapping("/preferences/language")
        @PreAuthorize("hasRole('FREELANCER') or hasRole('CLIENT') or hasRole('ADMIN')")
        public ResponseEntity<List<User>> getUsersByLanguageAndMinimumCompletedContracts(
                @RequestParam String lang,
                @RequestParam(defaultValue = "0") Long minContracts) {
            try {
                return ResponseEntity.ok(userService.findUsersByLanguageAndMinimumCompletedContracts(lang, minContracts));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        
        }
}