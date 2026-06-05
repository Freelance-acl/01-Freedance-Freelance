package com.team01.freelance.proposal.dto;

import com.team01.freelance.proposal.model.MilestoneStatus;
import com.team01.freelance.proposal.model.ProposalStatus;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DtoBuilderPatternTest {

    @Test
    void feeEstimateDtoExposesDp4BuilderContract() throws Exception {
        assertBuilderContract(FeeEstimateDTO.class,
                new MethodSpec("bidAmount", double.class),
                new MethodSpec("platformFee", double.class),
                new MethodSpec("freelancerPayout", double.class),
                new MethodSpec("feePercentage", int.class),
                new MethodSpec("estimatedDailyRate", double.class));
    }

    @Test
    void proposalAnalyticsDtoExposesDp4BuilderContract() throws Exception {
        assertBuilderContract(ProposalAnalyticsDTO.class,
                new MethodSpec("totalProposals", long.class),
                new MethodSpec("acceptedProposals", long.class),
                new MethodSpec("rejectedProposals", long.class),
                new MethodSpec("totalBidValue", double.class),
                new MethodSpec("averageBid", double.class),
                new MethodSpec("acceptanceRate", double.class));
    }

    @Test
    void proposalDetailsDtoExposesDp4BuilderContract() throws Exception {
        assertBuilderContract(ProposalDetailsDTO.class,
                new MethodSpec("proposalId", Long.class),
                new MethodSpec("jobId", Long.class),
                new MethodSpec("freelancerId", Long.class),
                new MethodSpec("status", ProposalStatus.class),
                new MethodSpec("bidAmount", Double.class),
                new MethodSpec("metadata", Map.class),
                new MethodSpec("milestones", List.class),
                new MethodSpec("totalMilestones", Integer.class),
                new MethodSpec("completedMilestones", Integer.class));
    }

    @Test
    void milestoneDtoExposesDp4BuilderContract() throws Exception {
        assertBuilderContract(ProposalDetailsDTO.MilestoneDTO.class,
                new MethodSpec("id", Long.class),
                new MethodSpec("milestoneOrder", Integer.class),
                new MethodSpec("title", String.class),
                new MethodSpec("description", String.class),
                new MethodSpec("amount", Double.class),
                new MethodSpec("status", MilestoneStatus.class),
                new MethodSpec("metadata", Map.class));
    }

    @Test
    void proposalAnalyticsBuilderPopulatesFields() {
        ProposalAnalyticsDTO dto = ProposalAnalyticsDTO.builder()
                .totalProposals(10)
                .acceptedProposals(4)
                .rejectedProposals(2)
                .totalBidValue(5000.0)
                .averageBid(500.0)
                .acceptanceRate(0.4)
                .build();

        assertEquals(10, dto.getTotalProposals());
        assertEquals(4, dto.getAcceptedProposals());
        assertEquals(2, dto.getRejectedProposals());
        assertEquals(5000.0, dto.getTotalBidValue());
        assertEquals(500.0, dto.getAverageBid());
        assertEquals(0.4, dto.getAcceptanceRate());
    }

    @Test
    void feeEstimateBuilderPopulatesFields() {
        FeeEstimateDTO dto = FeeEstimateDTO.builder()
                .bidAmount(1000.0)
                .platformFee(150.0)
                .freelancerPayout(850.0)
                .feePercentage(15)
                .estimatedDailyRate(85.0)
                .build();

        assertEquals(1000.0, dto.getBidAmount());
        assertEquals(150.0, dto.getPlatformFee());
        assertEquals(850.0, dto.getFreelancerPayout());
        assertEquals(15, dto.getFeePercentage());
        assertEquals(85.0, dto.getEstimatedDailyRate());
    }

    @Test
    void proposalDetailsBuilderPopulatesFields() {
        ProposalDetailsDTO.MilestoneDTO milestone = ProposalDetailsDTO.MilestoneDTO.builder()
                .id(1L)
                .milestoneOrder(1)
                .title("Phase 1")
                .description("Setup")
                .amount(100.0)
                .status(MilestoneStatus.PENDING)
                .metadata(Map.of("k", "v"))
                .build();

        ProposalDetailsDTO dto = ProposalDetailsDTO.builder()
                .proposalId(9L)
                .jobId(3L)
                .freelancerId(7L)
                .status(ProposalStatus.SUBMITTED)
                .bidAmount(250.0)
                .metadata(Map.of("source", "test"))
                .milestones(List.of(milestone))
                .totalMilestones(1)
                .completedMilestones(0)
                .build();

        assertEquals(9L, dto.getProposalId());
        assertEquals(3L, dto.getJobId());
        assertEquals(7L, dto.getFreelancerId());
        assertEquals(ProposalStatus.SUBMITTED, dto.getStatus());
        assertEquals(250.0, dto.getBidAmount());
        assertEquals(1, dto.getTotalMilestones());
        assertEquals(milestone, dto.getMilestones().get(0));
    }

    private static void assertBuilderContract(Class<?> dtoClass, MethodSpec... fluentMethods) throws Exception {
        Method builderMethod = dtoClass.getMethod("builder");

        assertTrue(Modifier.isStatic(builderMethod.getModifiers()),
                dtoClass.getSimpleName() + ".builder() must be static");

        Object builder = builderMethod.invoke(null);
        assertNotNull(builder);

        Class<?> builderClass = builder.getClass();
        for (MethodSpec fluentMethod : fluentMethods) {
            Method method = builderClass.getMethod(fluentMethod.name(), fluentMethod.parameterType());
            assertEquals(builderClass, method.getReturnType());
        }

        Method buildMethod = builderClass.getMethod("build");
        assertEquals(dtoClass, buildMethod.getReturnType());
    }

    private record MethodSpec(String name, Class<?> parameterType) {
    }
}
