package com.team01.freelance.proposal.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team01.freelance.proposal.model.Proposal;
import com.team01.freelance.proposal.model.ProposalStatus;
import com.team01.freelance.proposal.repository.ProposalRepository;
import com.team01.freelance.proposal.support.AbstractIntegrationTest;
import com.team01.freelance.proposal.support.ProposalTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

/**
 * [S3-F3] Integration tests for {@code POST /api/proposals/estimate}.
 */
class ProposalFeeEstimateIntegrationTest extends AbstractIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ProposalRepository proposalRepository;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(webApplicationContext);
        proposalRepository.deleteAll();
    }

    @Test
    void estimateWithNoCompetition_returnsTwentyPercentFee() throws Exception {
        long countBefore = proposalRepository.count();

        mockMvc.perform(post("/api/proposals/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bidAmount":1000,"estimatedDays":10}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bidAmount").value(1000))
                .andExpect(jsonPath("$.feePercentage").value(20))
                .andExpect(jsonPath("$.platformFee").value(200))
                .andExpect(jsonPath("$.freelancerPayout").value(800))
                .andExpect(jsonPath("$.estimatedDailyRate").value(80));

        assertThat(proposalRepository.count()).isEqualTo(countBefore);
    }

    @Test
    void estimateWithModerateCompetition_returnsFifteenPercentFee() throws Exception {
        for (int i = 0; i < 10; i++) {
            saveSubmittedProposal(800.0 + (i * 40));
        }

        mockMvc.perform(post("/api/proposals/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bidAmount":1000,"estimatedDays":10}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feePercentage").value(15))
                .andExpect(jsonPath("$.platformFee").value(150))
                .andExpect(jsonPath("$.freelancerPayout").value(850))
                .andExpect(jsonPath("$.estimatedDailyRate").value(85));

        assertThat(proposalRepository.count()).isEqualTo(10);
    }

    @Test
    void estimateWithInvalidBidAmount_returns400() throws Exception {
        mockMvc.perform(post("/api/proposals/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bidAmount":0,"estimatedDays":10}
                                """))
                .andExpect(status().isBadRequest());
    }

    private void saveSubmittedProposal(double bidAmount) {
        Proposal proposal = new Proposal();
        proposal.setJobId(ProposalTestData.JOB_ID);
        proposal.setFreelancerId(ProposalTestData.FREELANCER_ID);
        proposal.setCoverLetter("Competing bid");
        proposal.setBidAmount(bidAmount);
        proposal.setEstimatedDays(10);
        proposal.setStatus(ProposalStatus.SUBMITTED);
        proposalRepository.save(proposal);
    }
}
