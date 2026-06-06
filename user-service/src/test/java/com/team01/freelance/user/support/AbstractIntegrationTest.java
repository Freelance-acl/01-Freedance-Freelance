package com.team01.freelance.user.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/**
 * Base for full-context tests using the {@code test} profile (embedded H2). For controller-only
 * tests, prefer {@link org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest}.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @Autowired(required = false)
    private CacheManager cacheManager;

    @BeforeEach
    void clearCaches() {
        if (cacheManager == null) {
            return;
        }
        cacheManager.getCacheNames().stream()
                .map(cacheManager::getCache)
                .filter(cache -> cache != null)
                .forEach(org.springframework.cache.Cache::clear);
    }

    protected static org.springframework.test.web.servlet.MockMvc buildMockMvc(WebApplicationContext context) {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }
}
