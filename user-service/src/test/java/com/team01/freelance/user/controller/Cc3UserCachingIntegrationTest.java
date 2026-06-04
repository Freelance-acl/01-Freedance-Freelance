package com.team01.freelance.user.controller;

import com.team01.freelance.user.cache.UserCacheKey;
import com.team01.freelance.user.cache.UserCacheNames;
import com.team01.freelance.user.model.ProficiencyLevel;
import com.team01.freelance.user.model.User;
import com.team01.freelance.user.model.UserRole;
import com.team01.freelance.user.model.UserSkill;
import com.team01.freelance.user.model.UserStatus;
import com.team01.freelance.user.repository.UserRepository;
import com.team01.freelance.user.repository.UserSkillRepository;
import com.team01.freelance.user.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@WithMockUser(roles = "ADMIN")
class Cc3UserCachingIntegrationTest extends AbstractIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSkillRepository userSkillRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User freelancer;
    private User client;
    private UserSkill skill;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(webApplicationContext);
        freelancer = saveUser("Cache Freelancer", UserRole.FREELANCER, "ar");
        client = saveUser("Cache Client", UserRole.CLIENT, "en");
        skill = saveSkill(freelancer);
        insertCompletedContract(freelancer.getId(), client.getId(), 3000.0,
                LocalDateTime.of(2026, 3, 10, 12, 0));
    }

    @Test
    void requiredGetEndpointsPopulateExpectedCaches() throws Exception {
        performTwice("/api/users/" + freelancer.getId());
        assertCached(UserCacheNames.USER, freelancer.getId());

        performTwice("/api/user-skills/" + skill.getId());
        assertCached(UserCacheNames.USER_SKILL, skill.getId());

        performTwice("/api/users/search?name=Cache");
        assertCached(UserCacheNames.S1_F1, UserCacheKey.hash("Cache", null, null));

        performTwice("/api/users/" + freelancer.getId() + "/contract-summary");
        assertCached(UserCacheNames.S1_F3, UserCacheKey.hash(freelancer.getId()));

        performTwice("/api/users/preferences/search?key=language&value=ar");
        assertCached(UserCacheNames.S1_F5, UserCacheKey.hash("language", "ar"));

        performTwice("/api/users/reports/top-freelancers?startDate=2026-03-01&endDate=2026-03-31&limit=2");
        assertCached(UserCacheNames.S1_F6,
                UserCacheKey.hash(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), 2));

        performTwice("/api/users/" + freelancer.getId() + "/profile");
        assertCached(UserCacheNames.S1_F8, UserCacheKey.hash(freelancer.getId()));

        performTwice("/api/users/preferences/language?lang=ar&minContracts=1");
        assertCached(UserCacheNames.S1_F9, UserCacheKey.hash("ar", 1L));
    }

    @Test
    void listEndpointsDoNotPopulateDetailCaches() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/user-skills"))
                .andExpect(status().isOk());

        assertNotCached(UserCacheNames.USER, freelancer.getId());
        assertNotCached(UserCacheNames.USER_SKILL, skill.getId());
    }

    @Test
    void userWriteInvalidatesDetailAndFeatureCaches() throws Exception {
        performTwice("/api/users/" + freelancer.getId());
        performTwice("/api/users/search?name=Cache");

        Object searchKey = UserCacheKey.hash("Cache", null, null);
        assertCached(UserCacheNames.USER, freelancer.getId());
        assertCached(UserCacheNames.S1_F1, searchKey);

        mockMvc.perform(put("/api/users/" + freelancer.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Updated Cache Freelancer"}
                                """))
                .andExpect(status().isOk());

        assertNotCached(UserCacheNames.USER, freelancer.getId());
        assertNotCached(UserCacheNames.S1_F1, searchKey);
    }

    @Test
    void userSkillWriteInvalidatesSkillAndProfileCaches() throws Exception {
        performTwice("/api/user-skills/" + skill.getId());
        performTwice("/api/users/" + freelancer.getId() + "/profile");

        Object profileKey = UserCacheKey.hash(freelancer.getId());
        assertCached(UserCacheNames.USER_SKILL, skill.getId());
        assertCached(UserCacheNames.S1_F8, profileKey);

        mockMvc.perform(put("/api/user-skills/" + skill.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"skillName":"Spring"}
                                """))
                .andExpect(status().isOk());

        assertNotCached(UserCacheNames.USER_SKILL, skill.getId());
        assertNotCached(UserCacheNames.S1_F8, profileKey);
    }

    private void performTwice(String path) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isOk());
        mockMvc.perform(get(path)).andExpect(status().isOk());
    }

    private void assertCached(String cacheName, Object key) {
        Cache cache = cacheManager.getCache(cacheName);
        assertNotNull(cache, "Cache should exist: " + cacheName);
        assertNotNull(cache.get(key), "Expected cache entry " + cacheName + "::" + key);
    }

    private void assertNotCached(String cacheName, Object key) {
        Cache cache = cacheManager.getCache(cacheName);
        assertNotNull(cache, "Cache should exist: " + cacheName);
        assertNull(cache.get(key), "Expected no cache entry " + cacheName + "::" + key);
    }

    private User saveUser(String name, UserRole role, String language) {
        long suffix = System.nanoTime();
        User user = new User();
        user.setName(name);
        user.setEmail("cc3-" + suffix + "@test.dev");
        user.setPassword("secret");
        user.setPhone("+201" + (suffix % 1_000_000_000L));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setPreferences(new LinkedHashMap<>(Map.of("language", language)));
        return userRepository.save(user);
    }

    private UserSkill saveSkill(User user) {
        UserSkill userSkill = new UserSkill();
        userSkill.setUser(user);
        userSkill.setSkillName("Java");
        userSkill.setCategory("Backend");
        userSkill.setYearsOfExperience(3);
        userSkill.setProficiencyLevel(ProficiencyLevel.EXPERT);
        userSkill.setIsPrimary(true);
        userSkill.setMetadata(new LinkedHashMap<>(Map.of("verified", true)));
        return userSkillRepository.save(userSkill);
    }

    private void insertCompletedContract(Long freelancerId, Long clientId, double amount, LocalDateTime endDate) {
        jdbcTemplate.update("""
                INSERT INTO contracts (job_id, freelancer_id, client_id, proposal_id, agreed_amount,
                                       status, start_date, end_date, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                1L, freelancerId, clientId, 3L, amount, "COMPLETED",
                Timestamp.valueOf(endDate.minusDays(5)),
                Timestamp.valueOf(endDate),
                Timestamp.valueOf(endDate));
    }
}
