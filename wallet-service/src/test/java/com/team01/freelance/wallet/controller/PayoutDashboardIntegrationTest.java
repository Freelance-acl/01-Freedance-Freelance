package com.team01.freelance.wallet.controller;

import com.team01.freelance.wallet.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PayoutDashboardIntegrationTest extends AbstractIntegrationTest {

    @Test
    void all_dtos_must_implement_strict_builder_pattern() throws Exception {
        String[] inScopeDtos = {
                "com.team01.freelance.wallet.dto.PayoutDetailsDTO",
                "com.team01.freelance.wallet.dto.PromoCodeUsageDTO"
        };

        for (String dtoName : inScopeDtos) {
            Class<?> dtoClass = Class.forName(dtoName);

            Method builderMethod = dtoClass.getMethod("builder");
            Object builder = builderMethod.invoke(null);
            assertNotNull(builder, "Class " + dtoName + " must have a static builder() method");

            boolean hasFluentSetter = false;
            for (Method m : builder.getClass().getMethods()) {
                if (m.getParameterCount() == 1 && m.getReturnType().equals(builder.getClass())) {
                    hasFluentSetter = true;
                    break;
                }
            }
            assertTrue(hasFluentSetter, "Builder for " + dtoName + " must have fluent setters returning the Builder instance");

            Method buildMethod = builder.getClass().getMethod("build");
            Object instance = buildMethod.invoke(builder);
            assertEquals(dtoClass, instance.getClass(), "build() must return the " + dtoName + " type");
        }
    }
}