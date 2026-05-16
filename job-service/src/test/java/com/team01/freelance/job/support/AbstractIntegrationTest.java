package com.team01.freelance.job.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base for full-context tests using the {@code test} profile (embedded H2). For controller-only
 * tests, prefer {@link org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest}.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {
}
