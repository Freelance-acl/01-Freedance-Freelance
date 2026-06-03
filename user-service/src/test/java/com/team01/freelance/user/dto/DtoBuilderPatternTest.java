package com.team01.freelance.user.dto;

import com.team01.freelance.user.model.ProficiencyLevel;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DtoBuilderPatternTest {

    @Test
    void m1UserDtosExposeDp4BuilderContract() throws Exception {
        assertBuilderContract(UserProfileSkillDTO.class,
                new MethodSpec("skillName", String.class),
                new MethodSpec("category", String.class),
                new MethodSpec("yearsOfExperience", Integer.class),
                new MethodSpec("proficiencyLevel", ProficiencyLevel.class),
                new MethodSpec("isPrimary", Boolean.class),
                new MethodSpec("metadata", Map.class)
        );

        assertBuilderContract(UserProfileDTO.class,
                new MethodSpec("userId", Long.class),
                new MethodSpec("name", String.class),
                new MethodSpec("email", String.class),
                new MethodSpec("phone", String.class),
                new MethodSpec("preferences", Map.class),
                new MethodSpec("skills", List.class),
                new MethodSpec("totalSkills", int.class)
        );

        assertBuilderContract(UserContractSummaryDTO.class,
                new MethodSpec("userId", Long.class),
                new MethodSpec("name", String.class),
                new MethodSpec("totalContracts", Long.class),
                new MethodSpec("completedContracts", Long.class),
                new MethodSpec("terminatedContracts", Long.class),
                new MethodSpec("totalEarnings", Double.class),
                new MethodSpec("averageContractValue", Double.class)
        );

        assertBuilderContract(TopFreelancerDTO.class,
                new MethodSpec("userId", Long.class),
                new MethodSpec("name", String.class),
                new MethodSpec("totalEarnings", BigDecimal.class),
                new MethodSpec("contractCount", Long.class)
        );
    }

    @Test
    void userProfileSkillBuilderPopulatesFields() {
        ProficiencyLevel level = ProficiencyLevel.values()[0];
        Map<String, Object> metadata = Map.of("certified", true);

        UserProfileSkillDTO dto = UserProfileSkillDTO.builder()
                .skillName("Java")
                .category("Backend")
                .yearsOfExperience(4)
                .proficiencyLevel(level)
                .isPrimary(true)
                .metadata(metadata)
                .build();

        assertEquals("Java", dto.getSkillName());
        assertEquals("Backend", dto.getCategory());
        assertEquals(4, dto.getYearsOfExperience());
        assertEquals(level, dto.getProficiencyLevel());
        assertEquals(true, dto.getIsPrimary());
        assertEquals(metadata, dto.getMetadata());
    }

    @Test
    void userProfileBuilderPopulatesFields() {
        UserProfileSkillDTO skill = UserProfileSkillDTO.builder()
                .skillName("SQL")
                .category("Database")
                .yearsOfExperience(3)
                .proficiencyLevel(ProficiencyLevel.values()[0])
                .isPrimary(false)
                .metadata(Map.of("source", "test"))
                .build();

        Map<String, Object> preferences = Map.of("language", "en");

        UserProfileDTO dto = UserProfileDTO.builder()
                .userId(10L)
                .name("Omar")
                .email("omar@example.com")
                .phone("01000000000")
                .preferences(preferences)
                .skills(List.of(skill))
                .totalSkills(1)
                .build();

        assertEquals(10L, dto.getUserId());
        assertEquals("Omar", dto.getName());
        assertEquals("omar@example.com", dto.getEmail());
        assertEquals("01000000000", dto.getPhone());
        assertEquals(preferences, dto.getPreferences());
        assertEquals(1, dto.getTotalSkills());
        assertSame(skill, dto.getSkills().get(0));
    }

    @Test
    void userContractSummaryBuilderPopulatesFields() {
        UserContractSummaryDTO dto = UserContractSummaryDTO.builder()
                .userId(7L)
                .name("Freelancer")
                .totalContracts(5L)
                .completedContracts(3L)
                .terminatedContracts(1L)
                .totalEarnings(1200.0)
                .averageContractValue(400.0)
                .build();

        assertEquals(7L, dto.getUserId());
        assertEquals("Freelancer", dto.getName());
        assertEquals(5L, dto.getTotalContracts());
        assertEquals(3L, dto.getCompletedContracts());
        assertEquals(1L, dto.getTerminatedContracts());
        assertEquals(1200.0, dto.getTotalEarnings());
        assertEquals(400.0, dto.getAverageContractValue());
    }

    @Test
    void topFreelancerBuilderPopulatesFields() {
        BigDecimal earnings = new BigDecimal("9500.75");

        TopFreelancerDTO dto = TopFreelancerDTO.builder()
                .userId(3L)
                .name("Top User")
                .totalEarnings(earnings)
                .contractCount(8L)
                .build();

        assertEquals(3L, dto.getUserId());
        assertEquals("Top User", dto.getName());
        assertEquals(earnings, dto.getTotalEarnings());
        assertEquals(8L, dto.getContractCount());
    }

    private static void assertBuilderContract(Class<?> dtoClass, MethodSpec... fluentMethods) throws Exception {
        Method builderMethod = dtoClass.getMethod("builder");

        assertTrue(Modifier.isStatic(builderMethod.getModifiers()),
                dtoClass.getSimpleName() + ".builder() must be static");

        Object builder = builderMethod.invoke(null);
        assertNotNull(builder, dtoClass.getSimpleName() + ".builder() must return a builder instance");

        Class<?> builderClass = builder.getClass();

        for (MethodSpec fluentMethod : fluentMethods) {
            Method method = builderClass.getMethod(fluentMethod.name(), fluentMethod.parameterType());

            assertEquals(builderClass, method.getReturnType(),
                    dtoClass.getSimpleName() + "." + fluentMethod.name()
                            + "() must return the Builder for fluent chaining");
        }

        Method buildMethod = builderClass.getMethod("build");

        assertEquals(dtoClass, buildMethod.getReturnType(),
                dtoClass.getSimpleName() + ".build() must return the DTO type");
    }

    private record MethodSpec(String name, Class<?> parameterType) {
    }
}