package com.team01.freelance.user.controller;

import com.jayway.jsonpath.JsonPath;
import com.team01.freelance.user.config.JwtConfig;
import com.team01.freelance.user.model.User;
import com.team01.freelance.user.model.UserRole;
import com.team01.freelance.user.repository.UserRepository;
import com.team01.freelance.user.support.AbstractIntegrationTest;
import com.team01.freelance.user.support.JwtTestSupport;
import com.team01.freelance.user.support.UserTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [4.x] Role enum, registration defaults, JWT role claim, and role update authorization.
 */
@Transactional
class UserRoleIntegrationTest extends AbstractIntegrationTest {

    private static final Set<String> EXPECTED_ROLES = Set.of("FREELANCER", "CLIENT", "ADMIN");

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JwtConfig jwtConfig;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(webApplicationContext);
        UserTestFixtures.seedAdmin(userRepository);
    }

    // (a) Role metadata contains FREELANCER, CLIENT, ADMIN
    @Test
    void roleMetadata_containsFreelancerClientAdmin() throws Exception {
        assertEquals(EXPECTED_ROLES,
                Set.of(UserRole.values()).stream().map(Enum::name).collect(Collectors.toSet()));

        if (usesPostgres()) {
            List<String> enumLabels = jdbcTemplate.queryForList("""
                    SELECT e.enumlabel
                    FROM pg_enum e
                    JOIN pg_type t ON e.enumtypid = t.oid
                    WHERE t.typname = (
                        SELECT udt_name
                        FROM information_schema.columns
                        WHERE table_schema = 'public'
                          AND table_name = 'users'
                          AND column_name = 'role'
                    )
                    ORDER BY e.enumsortorder
                    """, String.class);
            if (!enumLabels.isEmpty()) {
                assertEquals(EXPECTED_ROLES, Set.copyOf(enumLabels));
                return;
            }
        }

        String dataType = jdbcTemplate.queryForObject("""
                SELECT data_type
                FROM information_schema.columns
                WHERE LOWER(table_name) = 'users' AND LOWER(column_name) = 'role'
                """, String.class);
        assertTrue(dataType != null && !dataType.isBlank());

        for (String role : EXPECTED_ROLES) {
            String email = "meta-" + role.toLowerCase() + "-" + UUID.randomUUID() + "@test.dev";
            jdbcTemplate.update(
                    "INSERT INTO users (name, email, password, phone, role, status, created_at) "
                            + "VALUES (?, ?, ?, ?, ?, 'ACTIVE', CURRENT_TIMESTAMP)",
                    "Meta " + role, email, UserTestFixtures.SEED_PASSWORD_HASH,
                    "+1999" + (System.nanoTime() % 10_000_000L), role);
        }
    }

    // (b) Register without role → CLIENT in DB
    @Test
    void register_withoutRole_defaultsToClientInDatabase() throws Exception {
        String email = uniqueEmail("no-role");

        register(email, registerBodyWithoutRole(email), status().isCreated());

        assertEquals("CLIENT", roleForEmail(email));
    }

    // (c) Register without role → JWT role claim CLIENT
    @Test
    void register_withoutRole_jwtRoleClaimIsClient() throws Exception {
        String email = uniqueEmail("jwt-client");

        String response = register(email, registerBodyWithoutRole(email), status().isCreated());
        String token = JsonPath.read(response, "$.token");

        assertEquals("CLIENT", JwtTestSupport.extractRoleClaim(token, jwtConfig));
    }

    @Test
    void register_jwtContainsSubUidRoleClaims() throws Exception {
        String email = uniqueEmail("jwt-claims");

        String response = register(email, registerBodyWithoutRole(email), status().isCreated());
        String token = JsonPath.read(response, "$.token");

        assertEquals(email, JwtTestSupport.extractSubjectClaim(token, jwtConfig));
        assertEquals("CLIENT", JwtTestSupport.extractRoleClaim(token, jwtConfig));

        Long dbId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?", Long.class, email);
        assertEquals(dbId, JwtTestSupport.extractUidClaim(token, jwtConfig));
    }

    // (d) Register with FREELANCER → FREELANCER in DB
    @Test
    void register_withFreelancerRole_persistsFreelancer() throws Exception {
        String email = uniqueEmail("freelancer");

        register(email, registerBodyWithRole(email, "FREELANCER"), status().isCreated());

        assertEquals("FREELANCER", roleForEmail(email));
    }

    // (e) Login as seeded ADMIN → JWT role ADMIN
    @Test
    void login_asSeededAdmin_jwtRoleClaimIsAdmin() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(UserTestFixtures.SEED_ADMIN_EMAIL, UserTestFixtures.SEED_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertEquals("ADMIN", JwtTestSupport.extractRoleClaim(JsonPath.read(response, "$.token"), jwtConfig));
    }

    // (f) Register with role ADMIN → ignored, still CLIENT
    @Test
    void register_withAdminRole_ignoredAndCreatesClient() throws Exception {
        String email = uniqueEmail("bad-admin");

        register(email, registerBodyWithRole(email, "ADMIN"), status().isCreated());

        assertEquals("CLIENT", roleForEmail(email));
    }

    // (g) CLIENT token → PUT /users/{id}/role → 403
    @Test
    void updateRole_withClientToken_returnsForbidden() throws Exception {
        User client = UserTestFixtures.saveUser(
                userRepository, "Client", uniqueEmail("client-403"),
                "+1888" + (System.nanoTime() % 10_000_000L), UserRole.CLIENT, UserTestFixtures.SEED_PASSWORD);
        User target = UserTestFixtures.saveUser(
                userRepository, "Target", uniqueEmail("target-403"),
                "+1999" + (System.nanoTime() % 10_000_000L), UserRole.CLIENT, UserTestFixtures.SEED_PASSWORD);

        String clientToken = loginToken(client.getEmail());

        mockMvc.perform(put("/api/users/{id}/role", target.getId())
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"FREELANCER\"}"))
                .andExpect(status().isForbidden());
    }

    // (h) ADMIN token → PUT /users/{id}/role {"role":"FREELANCER"} → 200
    @Test
    void updateRole_withAdminToken_changesTargetRole() throws Exception {
        User target = UserTestFixtures.saveUser(
                userRepository, "Role Target", uniqueEmail("role-target"),
                "+1777" + (System.nanoTime() % 10_000_000L), UserRole.CLIENT, UserTestFixtures.SEED_PASSWORD);

        String adminToken = loginToken(UserTestFixtures.SEED_ADMIN_EMAIL);

        mockMvc.perform(put("/api/users/{id}/role", target.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"FREELANCER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("FREELANCER"));

        assertEquals(UserRole.FREELANCER,
                userRepository.findById(target.getId()).orElseThrow().getRole());
    }

    // (i) CLIENT token → regular user endpoint (S1-F1 search) → 200
    @Test
    void search_withClientToken_succeeds() throws Exception {
        User client = UserTestFixtures.saveUser(
                userRepository, "Search Client", uniqueEmail("search-client"),
                "+1666" + (System.nanoTime() % 10_000_000L), UserRole.CLIENT, UserTestFixtures.SEED_PASSWORD);

        mockMvc.perform(get("/api/users/search").param("role", "CLIENT")
                        .header("Authorization", "Bearer " + loginToken(client.getEmail())))
                .andExpect(status().isOk());
    }

    private String register(String email, String body, org.springframework.test.web.servlet.ResultMatcher expectedStatus)
            throws Exception {
        return mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(expectedStatus)
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private String loginToken(String email) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, UserTestFixtures.SEED_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(response, "$.token");
    }

    private String roleForEmail(String email) {
        return jdbcTemplate.queryForObject(
                "SELECT role FROM users WHERE email = ?", String.class, email);
    }

    private String roleForUserId(Long id) {
        return jdbcTemplate.queryForObject(
                "SELECT role FROM users WHERE id = ?", String.class, id);
    }

    private boolean usesPostgres() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            return "PostgreSQL".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName());
        }
    }

    private static String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@test.dev";
    }

    private static String registerBodyWithoutRole(String email) {
        return """
                {
                  "name": "Test User",
                  "email": "%s",
                  "password": "%s",
                  "phone": "+15551234567"
                }
                """.formatted(email, UserTestFixtures.SEED_PASSWORD);
    }

    private static String registerBodyWithRole(String email, String role) {
        return """
                {
                  "name": "Test User",
                  "email": "%s",
                  "password": "%s",
                  "phone": "+15551234568",
                  "role": "%s"
                }
                """.formatted(email, UserTestFixtures.SEED_PASSWORD, role);
    }
}
