package com.team01.freelance.wallet.dto;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;

public class BuilderReflectionTest {

    @Test
    void testPayoutDetailsDTOBuilderPattern() throws Exception {
        Class<PayoutDetailsDTO> dtoClass = PayoutDetailsDTO.class;

        Method builderMethod = dtoClass.getMethod("builder");
        Object builder = builderMethod.invoke(null);
        assertNotNull(builder, "Builder method returned null");
        assertEquals(PayoutDetailsDTO.Builder.class, builder.getClass());

        Method setterMethod = builder.getClass().getMethod("payoutId", Long.class);
        Object returnedBuilder = setterMethod.invoke(builder, 1L);
        assertEquals(builder, returnedBuilder, "Setter must return the Builder instance (fluent interface)");

        Method buildMethod = builder.getClass().getMethod("build");
        Object instance = buildMethod.invoke(builder);
        assertTrue(instance instanceof PayoutDetailsDTO, "build() must return a PayoutDetailsDTO instance");
    }

    @Test
    void testS5F9_BuilderPatternExists() throws Exception {
        Class<?> dtoClass = PromoCodeUsageDTO.class;

        Method builderMethod = dtoClass.getMethod("builder");
        Object builder = builderMethod.invoke(null);
        assertNotNull(builder);

        Method buildMethod = builder.getClass().getMethod("build");
        Object instance = buildMethod.invoke(builder);
        assertTrue(instance instanceof PromoCodeUsageDTO);
    }
}