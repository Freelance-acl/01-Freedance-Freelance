package com.team01.freelance.wallet.dto;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;

public class BuilderReflectionTest {

    @Test
    void testPayoutDetailsDTOBuilderPattern() throws Exception {
        Class<PayoutDetailsDTO> dtoClass = PayoutDetailsDTO.class;

        // 1. Assert static builder() exists and returns an instance of the Builder
        Method builderMethod = dtoClass.getMethod("builder");
        Object builder = builderMethod.invoke(null);
        assertNotNull(builder, "Builder method returned null");
        assertEquals(PayoutDetailsDTO.Builder.class, builder.getClass());

        // 2. Assert builder has fluent setter (e.g., payoutId) and build() method
        Method setterMethod = builder.getClass().getMethod("payoutId", Long.class);
        Object returnedBuilder = setterMethod.invoke(builder, 1L);
        assertEquals(builder, returnedBuilder, "Setter must return the Builder instance (fluent interface)");

        // 3. Assert build() returns PayoutDetailsDTO
        Method buildMethod = builder.getClass().getMethod("build");
        Object instance = buildMethod.invoke(builder);
        assertTrue(instance instanceof PayoutDetailsDTO, "build() must return a PayoutDetailsDTO instance");
    }
}