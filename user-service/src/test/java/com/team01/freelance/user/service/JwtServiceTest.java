package com.team01.freelance.user.service;

import com.team01.freelance.user.config.JwtConfigurationManager;
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

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtConfigurationManager.resetForTests(VALID_SECRET, 3_600_000L);
        jwtService = new JwtService();
    }

    @Test
    void generateToken_includesSubUidRoleIatExp() {
        User user = sampleUser(42L, "alice@freelance.com", UserRole.FREELANCER);

        String token = jwtService.generateToken(user);

        assertEquals("alice@freelance.com", jwtService.extractUsername(token));
        assertEquals(42L, jwtService.extractUserId(token));
        assertEquals("FREELANCER", jwtService.extractRole(token));
        assertEquals("alice@freelance.com", JwtTestSupport.extractSubjectClaim(token));
        assertEquals(42L, JwtTestSupport.extractUidClaim(token));
        assertEquals("FREELANCER", JwtTestSupport.extractRoleClaim(token));
        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    void blankSecret_rejectedAtStartup() {
        assertThrows(IllegalStateException.class,
                () -> JwtConfigurationManager.resetForTests("   ", 3_600_000L));
    }

    @Test
    void shortSecret_rejectedAtStartup() {
        assertThrows(IllegalStateException.class,
                () -> JwtConfigurationManager.resetForTests("bXlTZWNyZXQxMjM=", 3_600_000L));
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
