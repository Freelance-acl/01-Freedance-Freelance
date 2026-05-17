package com.team01.freelance.user.service;

import com.team01.freelance.user.model.User;
import com.team01.freelance.user.model.UserStatus;
import com.team01.freelance.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.team01.freelance.user.model.UserRole;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import org.springframework.transaction.annotation.Transactional;
import com.team01.freelance.user.dto.UserContractSummaryDTO;
import com.team01.freelance.user.dto.UserProfileDTO;
import com.team01.freelance.user.dto.UserProfileSkillDTO;
import com.team01.freelance.user.model.UserSkill;
import com.team01.freelance.user.repository.UserSkillRepository;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSkillRepository userSkillRepository;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public User createUser(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public User deactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));

        if (userRepository.countActiveContractsForUser(id) > 0) {
            throw new IllegalStateException("Cannot deactivate user with active contracts");
        }

        user.setStatus(UserStatus.DEACTIVATED);
        userRepository.withdrawSubmittedProposalsForUser(id);
        return userRepository.save(user);
    }

    /**
     * Updates an existing user and throws if it does not exist.
     *
     * @param id The ID of the user to update
     * @param userDetails The object containing updated fields
     * @return The updated user
     * @throws EntityNotFoundException if the user is not found
     */
    public User updateUser(Long id, User userDetails) {
        return userRepository.findById(id).map(existingUser -> {
            if (userDetails.getId() != null && !id.equals(userDetails.getId())) {
                throw new IllegalArgumentException("User ID cannot be changed. Use the path ID only.");
            }
            if (userDetails.getName() != null) existingUser.setName(userDetails.getName());
            if (userDetails.getEmail() != null) existingUser.setEmail(userDetails.getEmail());
            if (userDetails.getPassword() != null) existingUser.setPassword(userDetails.getPassword());
            if (userDetails.getPhone() != null) existingUser.setPhone(userDetails.getPhone());
            if (userDetails.getRole() != null) existingUser.setRole(userDetails.getRole());
            if (userDetails.getStatus() != null) existingUser.setStatus(userDetails.getStatus());
            if (userDetails.getPreferences() != null) existingUser.setPreferences(userDetails.getPreferences());
            if (userDetails.getCreatedAt() != null) existingUser.setCreatedAt(userDetails.getCreatedAt());
            return userRepository.save(existingUser);
        }).orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
    }

    public boolean deleteUserById(Long id) {
        if (!userRepository.existsById(id)) {
            return false;
        }
        userRepository.deleteById(id);
        return true;
    }

    public void deleteAllUsers() {
        userRepository.deleteAll();
    }

    public List<User> searchUsers(String name, String email, UserRole role) {
        return userRepository.searchUsers(name, email, role);
    }

    @Transactional
    public User updatePreferences(Long id, Map<String, Object> incomingPreferences) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));

        Map<String, Object> merged = user.getPreferences() != null
                ? new HashMap<>(user.getPreferences())
                : new HashMap<>();

        if (incomingPreferences != null) {
            merged.putAll(incomingPreferences);
        }

        user.setPreferences(merged);
        return userRepository.save(user);
    }
    public UserProfileDTO getUserProfile(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));

        List<UserSkill> userSkills = userSkillRepository.findByUserId(id);

        List<UserProfileSkillDTO> skills = userSkills.stream()
                .map(skill -> new UserProfileSkillDTO(
                        skill.getSkillName(),
                        skill.getCategory(),
                        skill.getYearsOfExperience(),
                        skill.getProficiencyLevel(),
                        skill.getIsPrimary() != null ? skill.getIsPrimary() : false,
                        skill.getMetadata()
                ))
                .toList();

        return new UserProfileDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getPreferences(),
                skills,
                skills.size()
        );
    }

    public List<User> findUsersByLanguageAndMinimumCompletedContracts(String lang, Long minContracts) {
        if (lang == null || lang.trim().isEmpty()) {
            throw new IllegalArgumentException("Language cannot be blank");
        }

        Long minimumContracts = minContracts != null ? minContracts : 0L;

        return userRepository.findUsersByLanguageAndMinimumCompletedContracts(
                lang.trim(),
                minimumContracts
        );
    }

    public UserContractSummaryDTO getUserContractSummary(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));

        Object result = userRepository.getUserContractSummary(id);
        if (result == null) {
            return zeroContractSummary(user);
        }
        Object[] row = (Object[]) result;

        Long totalContracts = row[0] != null ? ((Number) row[0]).longValue() : 0L;
        Long completedContracts = row[1] != null ? ((Number) row[1]).longValue() : 0L;
        Long terminatedContracts = row[2] != null ? ((Number) row[2]).longValue() : 0L;
        Double totalEarnings = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
        Double averageContractValue = row[4] != null ? ((Number) row[4]).doubleValue() : 0.0;

        return new UserContractSummaryDTO(
                user.getId(),
                user.getName(),
                totalContracts,
                completedContracts,
                terminatedContracts,
                totalEarnings,
                averageContractValue
        );
    }

    private static UserContractSummaryDTO zeroContractSummary(User user) {
        return new UserContractSummaryDTO(
                user.getId(),
                user.getName(),
                0L,
                0L,
                0L,
                0.0,
                0.0
        );
    }
}
