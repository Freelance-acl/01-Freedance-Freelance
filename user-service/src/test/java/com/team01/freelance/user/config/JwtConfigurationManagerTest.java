package com.team01.freelance.user.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtConfigurationManagerTest {

    private static final String SECRET = "dGhpcyBpcyBhIHNlY3JldCBrZXkgZm9yIEpXVCBzaWduaW5n";

    @AfterEach
    void tearDown() {
        JwtConfigurationManager.resetForTests(SECRET, 3_600_000L);
    }

    @Test
    void getInstance_returnsSingleton() {
        JwtConfigurationManager.resetForTests(SECRET, 3_600_000L);
        JwtConfigurationManager first = JwtConfigurationManager.getInstance();
        JwtConfigurationManager second = JwtConfigurationManager.getInstance();
        assertSame(first, second);
    }

    @Test
    void constructor_isPrivate() throws NoSuchMethodException {
        Constructor<JwtConfigurationManager> constructor =
                JwtConfigurationManager.class.getDeclaredConstructor(String.class, long.class);
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
    }

    @Test
    void getInstance_beforeConfigure_throws() throws Exception {
        java.lang.reflect.Field field = JwtConfigurationManager.class.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, null);
        assertThrows(IllegalStateException.class, JwtConfigurationManager::getInstance);
    }

    @Test
    void configure_providesSigningKey() {
        JwtConfigurationManager.resetForTests(SECRET, 3_600_000L);
        assertNotNull(JwtConfigurationManager.getInstance().getSigningKey());
    }
}
