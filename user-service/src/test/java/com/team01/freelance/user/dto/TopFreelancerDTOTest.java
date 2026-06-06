package com.team01.freelance.user.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TopFreelancerDTOTest {

    @Test
    void builderPopulatesAllS1F6ReportFields() {
        TopFreelancerDTO dto = TopFreelancerDTO.builder()
                .userId(7L)
                .name("Seif Freelancer")
                .totalEarnings(new BigDecimal("9500.50"))
                .contractCount(3L)
                .build();

        assertEquals(7L, dto.getUserId());
        assertEquals("Seif Freelancer", dto.getName());
        assertEquals(new BigDecimal("9500.50"), dto.getTotalEarnings());
        assertEquals(3L, dto.getContractCount());
    }
}
