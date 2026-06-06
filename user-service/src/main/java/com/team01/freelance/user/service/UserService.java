package com.team01.freelance.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team01.freelance.common.observer.EventSubject;
import com.team01.freelance.user.cache.UserCacheNames;
import com.team01.freelance.user.adapter.MongoDocumentAdapter;
import com.team01.freelance.user.dto.TopFreelancerDTO;
import com.team01.freelance.user.dto.UserActivityEventDTO;
import com.team01.freelance.user.dto.UserActivityFeedDTO;
import com.team01.freelance.user.dto.UserContractSummaryDTO;
import com.team01.freelance.user.dto.UserProfileDTO;
import com.team01.freelance.user.dto.UserProfileSkillDTO;
import com.team01.freelance.user.event.AuthEvent;
import com.team01.freelance.user.model.User;
import com.team01.freelance.user.model.UserRole;
import com.team01.freelance.user.model.UserSkill;
import com.team01.freelance.user.model.UserStatus;
import com.team01.freelance.user.repository.AuthEventRepository;
import com.team01.freelance.user.repository.UserRepository;
import com.team01.freelance.user.repository.UserSkillRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import com.team01.freelance.user.dto.UserContractSummaryDTO;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.springframework.transaction.annotation.Transactional;
import com.team01.freelance.user.dto.UserContractSummaryDTO;
import com.team01.freelance.user.dto.UserProfileDTO;
import com.team01.freelance.user.dto.UserProfileSkillDTO;
import com.team01.freelance.user.model.UserSkill;
import com.team01.freelance.user.repository.UserSkillRepository;
import com.team01.freelance.user.adapter.ObjectArrayDtoAdapter;
import java.util.Optional;

@Service
public class UserService {

    private static final int DEFAULT_ACTIVITY_PAGE = 0;
    private static final int DEFAULT_ACTIVITY_SIZE = 10;
    private static final int MAX_ACTIVITY_SIZE = 100;
    private static final Duration ACTIVITY_CACHE_TTL = Duration.ofMinutes(5);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSkillRepository userSkillRepository;

    @Autowired
    private AuthEventRepository authEventRepository;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private EventSubject authEventSubject;

    @Autowired
    private ObjectArrayDtoAdapter objectArrayDtoAdapter;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MongoDocumentAdapter mongoDocumentAdapter;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Cacheable(value = UserCacheNames.USER, key = "#id", unless = "#result == null")
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Caching(evict = {
            @CacheEvict(value = UserCacheNames.USER, allEntries = true),
            @CacheEvict(value = UserCacheNames.S1_F1, allEntries = true),
            @CacheEvict(value = UserCacheNames.S1_F3, allEntries = true),
            @CacheEvict(value = UserCacheNames.S1_F5, allEntries = true),
            @CacheEvict(value = UserCacheNames.S1_F6, allEntries = true),
            @CacheEvict(value = UserCacheNames.S1_F8, allEntries = true),
            @CacheEvict(value = UserCacheNames.S1_F9, allEntries = true)
    })
    public User createUser(User user) {
        User saved = userRepository.save(user);
        authEventSubject.notifyObservers("USER_CREATED", Map.of(
                "userId", saved.getId(),
                "action", "USER_CREATED",
                "details", Map.of()));
        return saved;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = UserCacheNames.USER, key = "#id"),
            @CacheEvict(value = UserCacheNames.S1_F1, allEntries = true),
            @CacheEvict(value = UserCacheNames.S1_F3, allEntries = true),
            @CacheEvict(value = UserCacheNames.S1_F5, allEntries = true),
            @CacheEvict(value = UserCacheNames.S1_F6, allEntries = true),
            @CacheEvict(value = UserCacheNames.S1_F8, allEntries = true),
            @CacheEvict(value = UserCacheNames.S1_F9, allEntries = true)
    })
    public User deactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));

        if (userRepository.countActiveContractsForUser(id) > 0) {
            throw new IllegalStateException("Cannot deactivate user with active contracts");
        }

        user.setStatus(UserStatus.DEACTIVATED);
        userRepository.withdrawSubmittedProposalsForUser(id);
        User saved = userRepository.save(user);

        Map<String, Object> eventDetails = new HashMap<>();
        if (saved.getEmail() != null) {
            eventDetails.put("email", saved.getEmail());
        }
        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("userId", saved.getId());
        eventPayload.put("action", "USER_DEACTIVATED");
        eventPayload.put("details", eventDetails);
        if (authEventSubject != null) {
            authEventSubject.notifyObservers("USER_DEACTIVATED", eventPayload);
        }

        return saved;
    }

    @Cacheable(
            value = UserCacheNames.S1_F5,
            key = "T(com.team01.freelance.user.cache.UserCacheKey).hash(#key, #value)",
            unless = "#result == null")
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

    @Cacheable(
            value = UserCacheNames.S1_F6,
            key = "T(com.team01.freelance.user.cache.UserCacheKey).hash(#startDate, #endDate, #limit)",
            unless = "#result == null")
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
    @Caching(evict = {
            @CacheEvict(value = UserCacheNames.USER, key = "#id"),
            @CacheEvict(value = UserCacheNames.S1_F1, allEntries = true),
            @CacheEvict(value = UserCacheNames.S1_F3, allEntries = true),
            @CacheEvict(value = UserCacheNames.S1_F5, allEntries = true),
            @CacheEvict(value = UserCacheNames.S1_F6, allEntries = true),
            @CacheEvict(value = UserCacheNames.S1_F8, allEntries = true),
            @CacheEvict(value = UserCacheNames.S1_F9, allEntries = true)
    })
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
            User saved = userRepository.save(existingUser);
            authEventSubject.notifyObservers("USER_UPDATED", Map.of(
                    "userId", saved.getId(),
                    "action", "USER_UPDATED",
                    "details", Map.of()));
            return saved;
        }).orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
    }

    @Caching(evict = {
            @CacheEvict(value = UserCacheNames.USER, key = "#id"),
            @CacheEvict(value = UserCacheNames.S1_F1, allEntries = true),
            @CacheEvict(value = UserCacheNames.S1_F8, allEntries = true)
    })
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

    @Caching(evict = {
            @CacheEvict(value = UserCacheNames.USER, key = "#id"),
            @CacheEvict(value = UserCacheNames.S1_F1, allEntries = true),
            @CacheEvict(value = UserCacheNames.S1_F3, allEntries = true),
            @CacheEvict(value = UserCacheNames.S1_F5, allEntries = true),
            @CacheEvict(value = UserCacheNames.S1_F6, allEntries = true),
            @CacheEvict(value = UserCacheNames.S1_F8, allEntries = true),
            @CacheEvict(value = UserCacheNames.S1_F9, allEntries = true)
    })
    public boolean deleteUserById(Long id) {
        if (!userRepository.existsById(id)) {
            return false;
        }

        userRepository.deleteById(id);
        authEventSubject.notifyObservers("USER_DELETED", Map.of(
                "userId", id,
                "action", "USER_DELETED",
                "details", Map.of()));
        return true;
    }

    @Caching(evict = {
            @CacheEvict(value = UserCacheNames.USER, allEntries = true),
            @CacheEvict(value = UserCacheNames.S1_F1, allEntries = true),
            @CacheEvict(value = UserCacheNames.S1_F3, allEntries = true),
            @CacheEvict(value = UserCacheNames.S1_F5, allEntries = true),
            @CacheEvict(value = UserCacheNames.S1_F6, allEntries = true),
            @CacheEvict(value = UserCacheNames.S1_F8, allEntries = true),
            @CacheEvict(value = UserCacheNames.S1_F9, allEntries = true)
    })
    public void deleteAllUsers() {
        userRepository.deleteAll();
    }

    @Cacheable(
            value = UserCacheNames.S1_F1,
            key = "T(com.team01.freelance.user.cache.UserCacheKey).hash(#name, #email, #role)",
            unless = "#result == null")
    public List<User> searchUsers(String name, String email, UserRole role) {
        return userRepository.searchUsers(name, email, role);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = UserCacheNames.USER, key = "#id"),
            @CacheEvict(value = UserCacheNames.S1_F1, allEntries = true),
            @CacheEvict(value = UserCacheNames.S1_F5, allEntries = true),
            @CacheEvict(value = UserCacheNames.S1_F8, allEntries = true),
            @CacheEvict(value = UserCacheNames.S1_F9, allEntries = true)
    })
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
        User saved = userRepository.save(user);
        authEventSubject.notifyObservers("USER_UPDATED", Map.of(
                "userId", saved.getId(),
                "action", "USER_UPDATED",
                "details", Map.of()));
        return saved;
    }

    @Cacheable(
            value = UserCacheNames.S1_F8,
            key = "T(com.team01.freelance.user.cache.UserCacheKey).hash(#id)",
            unless = "#result == null")
    public UserProfileDTO getUserProfile(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));

        List<UserSkill> userSkills = userSkillRepository.findByUserId(id);

        List<UserProfileSkillDTO> skills = userSkills.stream()
                .map(skill -> UserProfileSkillDTO.builder()
                        .skillName(skill.getSkillName())
                        .category(skill.getCategory())
                        .yearsOfExperience(skill.getYearsOfExperience())
                        .proficiencyLevel(skill.getProficiencyLevel())
                        .isPrimary(skill.getIsPrimary() != null ? skill.getIsPrimary() : false)
                        .metadata(skill.getMetadata())
                        .build())
                .toList();

        return UserProfileDTO.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .preferences(user.getPreferences())
                .skills(skills)
                .totalSkills(skills.size())
                .build();
    }

    public UserActivityFeedDTO getUserActivityFeed(
            Long id,
            Integer page,
            Integer size,
            String authorizationHeader) {

        int safePage = normalizeActivityPage(page);
        int safeSize = normalizeActivitySize(size);

        String token = extractBearerToken(authorizationHeader);
        validateActivityFeedAccess(id, token);

        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + id);
        }

        String cacheKey = activityFeedCacheKey(id, safePage, safeSize);
        UserActivityFeedDTO cached = readActivityFeedFromCache(cacheKey);
        if (cached != null) {
            return cached;
        }

        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<AuthEvent> eventPage = authEventRepository.findByUserIdOrderByTimestampDesc(id, pageable);

        List<UserActivityEventDTO> events = eventPage.getContent().stream()
                .map(mongoDocumentAdapter::adapt)
                .toList();

        UserActivityFeedDTO response = UserActivityFeedDTO.builder()
                .content(events)
                .page(safePage)
                .size(safeSize)
                .totalElements(eventPage.getTotalElements())
                .totalPages(eventPage.getTotalPages())
                .build();

        writeActivityFeedToCache(cacheKey, response);
        return response;
    }

    @Cacheable(
            value = UserCacheNames.S1_F9,
            key = "T(com.team01.freelance.user.cache.UserCacheKey).hash(#lang, #minContracts)",
            unless = "#result == null")
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

    @Cacheable(
        value = UserCacheNames.S1_F3,
        key = "T(com.team01.freelance.user.cache.UserCacheKey).hash(#id)",
        unless = "#result == null")
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

        return UserContractSummaryDTO.builder()
                .userId(user.getId())
                .name(user.getName())
                .totalContracts(totalContracts)
                .completedContracts(completedContracts)
                .terminatedContracts(terminatedContracts)
                .totalEarnings(totalEarnings)
                .averageContractValue(averageContractValue)
                .build();
    }
    private static UserContractSummaryDTO zeroContractSummary(User user) {
        return UserContractSummaryDTO.builder()
                .userId(user.getId())
                .name(user.getName())
                .totalContracts(0L)
                .completedContracts(0L)
                .terminatedContracts(0L)
                .totalEarnings(0.0)
                .averageContractValue(0.0)
                .build();
    }

    private static int normalizeActivityPage(Integer page) {
        if (page == null || page < 0) {
            return DEFAULT_ACTIVITY_PAGE;
        }
        return page;
    }

    private static int normalizeActivitySize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_ACTIVITY_SIZE;
        }
        return Math.min(size, MAX_ACTIVITY_SIZE);
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing Bearer token");
        }

        String token = authorizationHeader.substring(7).trim();
        if (token.isEmpty() || !jwtService.isTokenValid(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }

        return token;
    }

    private void validateActivityFeedAccess(Long requestedUserId, String token) {
        try {
            Long callerUserId = jwtService.extractUserId(token);
            String role = jwtService.extractRole(token);

            boolean isOwner = requestedUserId != null && requestedUserId.equals(callerUserId);
            boolean isAdmin = "ADMIN".equals(role) || "ROLE_ADMIN".equals(role);

            if (!isOwner && !isAdmin) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot access another user's activity feed");
            }
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token claims");
        }
    }

    private static String activityFeedCacheKey(Long userId, int page, int size) {
        return "user-service::S1-F12::" + userId + "::page=" + page + "::size=" + size;
    }

    private UserActivityFeedDTO readActivityFeedFromCache(String cacheKey) {
        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson == null || cachedJson.isBlank()) {
                return null;
            }
            return objectMapper.readValue(cachedJson, UserActivityFeedDTO.class);
        } catch (Exception ex) {
            return null;
        }
    }

    private void writeActivityFeedToCache(String cacheKey, UserActivityFeedDTO response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(cacheKey, json, ACTIVITY_CACHE_TTL);
        } catch (Exception ignored) {
            // Cache failures must not break the activity feed endpoint.
        }
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
