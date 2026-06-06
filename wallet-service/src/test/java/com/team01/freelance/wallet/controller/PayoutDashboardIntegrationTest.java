package com.team01.freelance.wallet.controller;

import com.team01.freelance.wallet.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PayoutDashboardIntegrationTest extends AbstractIntegrationTest {

    @Test
    void payout_details_dto_has_valid_builder_contract() throws Exception {

        Class<?> dtoClass = Class.forName(
                "com.team01.freelance.wallet.dto.PayoutDetailsDTO"
        );

        Method builderMethod = dtoClass.getMethod("builder");
        Object builder = builderMethod.invoke(null);
        assertNotNull(builder);

        Method buildMethod = builder.getClass().getMethod("build");
        assertEquals(dtoClass, buildMethod.getReturnType());

        boolean fluent = true;

        for (Method m : builder.getClass().getMethods()) {

            if (m.getName().equals("build")) continue;

            if (m.getReturnType().equals(builder.getClass())
                    && m.getParameterCount() >= 1) {
                continue;
            }

            if (m.getDeclaringClass().equals(Object.class)) continue;
        }

        boolean hasFluent = List.of(builder.getClass().getMethods()).stream()
                .anyMatch(m ->
                        !m.getName().equals("build")
                                && m.getReturnType().equals(builder.getClass())
                                && m.getParameterCount() >= 1
                );

        assertTrue(hasFluent, "Builder must support fluent chaining");
    }
}