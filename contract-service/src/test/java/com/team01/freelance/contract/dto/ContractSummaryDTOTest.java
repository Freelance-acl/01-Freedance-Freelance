package com.team01.freelance.contract.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContractSummaryDTOTest {

    @Test
    void builderPopulatesAllFields() {
        ContractSummaryDTO dto = ContractSummaryDTO.builder()
                .contractId(17L)
                .freelancerName("Mariam")
                .jobTitle("Search API")
                .agreedAmount(4200.0)
                .status("ACTIVE")
                .durationDays(6L)
                .build();

        assertEquals(17L, dto.getContractId());
        assertEquals("Mariam", dto.getFreelancerName());
        assertEquals("Search API", dto.getJobTitle());
        assertEquals(4200.0, dto.getAgreedAmount());
        assertEquals("ACTIVE", dto.getStatus());
        assertEquals(6L, dto.getDurationDays());
    }
}
