package com.team01.freelance.user.service;

import com.team01.freelance.common.observer.EventSubject;
import com.team01.freelance.user.dto.TopFreelancerDTO;
import com.team01.freelance.user.model.User;
import com.team01.freelance.user.model.UserStatus;
import com.team01.freelance.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import com.team01.freelance.user.model.UserRole;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalTime;
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

    @Autowired
    private DataSource dataSource;

    @Autowired
    private EventSubject authEventSubject;

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

    public List<User> findUsersByPreference(String key, String value) {
        if (isBlank(key) || isBlank(value)) {
            throw new IllegalArgumentException("Preference key and value must not be blank");
        }
        String trimmedKey = key.trim();
        String trimmedValue = value.trim();
        if (!usesPostgresDatabase()) {
            return userRepository.findAll().stream()
                    .filter(user -> user.getPreferences() != null
                            && trimmedValue.equals(String.valueOf(user.getPreferences().get(trimmedKey))))
                    .toList();
        }
        return userRepository.findByPreference(trimmedKey, trimmedValue);
    }

    public List<TopFreelancerDTO> getTopFreelancersByEarnings(LocalDate startDate, LocalDate endDate, Integer limit) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate must be on or before endDate");
        }

        int queryLimit = limit == null ? 10 : limit;
        if (queryLimit <= 0) {
            throw new IllegalArgumentException("limit must be greater than zero");
        }

        return userRepository.findTopFreelancersByEarnings(
                        startDate.atStartOfDay(),
                        endDate.atTime(LocalTime.MAX),
                        queryLimit)
                .stream()
                .map(this::toTopFreelancerDTO)
                .toList();
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

    public User updateUserRole(Long id, UserRole role) {
        if (role == null) {
            throw new IllegalArgumentException("role must not be null");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
        UserRole oldRole = user.getRole();
        user.setRole(role);
        User saved = userRepository.save(user);

        authEventSubject.notifyObservers("ROLE_CHANGED", Map.of(
                "userId", saved.getId(),
                "action", "ROLE_CHANGED",
                "details", Map.of(
                        "oldRole", oldRole.name(),
                        "newRole", saved.getRole().name())));

        return saved;
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
        String language = lang.trim();

        if (!usesPostgresDatabase()) {
            return userRepository.findAll().stream()
                    .filter(user -> user.getPreferences() != null
                            && language.equalsIgnoreCase(String.valueOf(user.getPreferences().get("language"))))
                    .filter(user -> completedContractCount(user.getId()) >= minimumContracts)
                    .toList();
        }

        return userRepository.findUsersByLanguageAndMinimumCompletedContracts(language, minimumContracts);
    }

    private long completedContractCount(Long userId) {
        Object result = userRepository.getUserContractSummary(userId);
        if (result == null) {
            return 0L;
        }
        Object[] row = (Object[]) result;
        return row[1] != null ? ((Number) row[1]).longValue() : 0L;
    }

    private boolean usesPostgresDatabase() {
        try (var connection = dataSource.getConnection()) {
            return "PostgreSQL".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName());
        } catch (Exception ex) {
            return false;
        }
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

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private TopFreelancerDTO toTopFreelancerDTO(Object[] row) {
        return TopFreelancerDTO.builder()
                .userId(toLong(row[0]))
                .name((String) row[1])
                .totalEarnings(toBigDecimal(row[2]))
                .contractCount(toLong(row[3]))
                .build();
    }

    private Long toLong(Object value) {
        if (value instanceof Long longValue) {
            return longValue;
        }
        if (value instanceof Integer integerValue) {
            return integerValue.longValue();
        }
        if (value instanceof BigInteger bigIntegerValue) {
            return bigIntegerValue.longValue();
        }
        if (value instanceof BigDecimal bigDecimalValue) {
            return bigDecimalValue.longValue();
        }
        if (value instanceof Number numberValue) {
            return numberValue.longValue();
        }
        return Long.valueOf(value.toString());
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal bigDecimalValue) {
            return bigDecimalValue;
        }
        if (value instanceof BigInteger bigIntegerValue) {
            return new BigDecimal(bigIntegerValue);
        }
        if (value instanceof Number numberValue) {
            return BigDecimal.valueOf(numberValue.doubleValue());
        }
        return new BigDecimal(value.toString());
    }
}
