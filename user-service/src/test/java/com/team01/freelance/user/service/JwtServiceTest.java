package com.team01.freelance.user.service;

import com.team01.freelance.user.config.JwtConfig;
import com.team01.freelance.user.model.User;
import com.team01.freelance.user.model.UserRole;
import com.team01.freelance.user.model.UserStatus;
import com.team01.freelance.user.support.JwtTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String VALID_SECRET = "dGhpcyBpcyBhIHNlY3JldCBrZXkgZm9yIEpXVCBzaWduaW5n";

    private JwtConfig jwtConfig;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtConfig = new JwtConfig();
        jwtConfig.setSecret(VALID_SECRET);
        jwtConfig.setExpiration(3_600_000L);
        jwtService = new JwtService(jwtConfig);
    }

    @Test
    void generateToken_includesSubUidRoleIatExp() {
        User user = sampleUser(42L, "alice@freelance.com", UserRole.FREELANCER);

        String token = jwtService.generateToken(user);

        assertEquals("alice@freelance.com", jwtService.extractUsername(token));
        assertEquals(42L, jwtService.extractUserId(token));
        assertEquals("FREELANCER", jwtService.extractRole(token));
        assertEquals("alice@freelance.com", JwtTestSupport.extractSubjectClaim(token, jwtConfig));
        assertEquals(42L, JwtTestSupport.extractUidClaim(token, jwtConfig));
        assertEquals("FREELANCER", JwtTestSupport.extractRoleClaim(token, jwtConfig));
        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    void blankSecret_rejectedAtStartup() {
        jwtConfig.setSecret("   ");

        IllegalStateException ex =
                assertThrows(IllegalStateException.class, () -> new JwtService(jwtConfig));
        assertTrue(ex.getMessage().contains("JWT_SECRET"));
    }

    @Test
    void shortSecret_rejectedAtStartup() {
        jwtConfig.setSecret("bXlTZWNyZXQxMjM="); // "mySecret123" — too few bytes for HS256

        IllegalStateException ex =
                assertThrows(IllegalStateException.class, () -> new JwtService(jwtConfig));
        assertTrue(ex.getMessage().contains("32 bytes"));
    }

    private static User sampleUser(Long id, String email, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setName("Test User");
        user.setEmail(email);
        user.setPassword("hashed");
        user.setPhone("+15551230000");
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}
