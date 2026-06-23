package com.team01.freelance.proposal.controller;

import com.team01.freelance.proposal.model.Proposal;
import com.team01.freelance.proposal.model.ProposalStatus;
import com.team01.freelance.proposal.repository.ProposalRepository;
import com.team01.freelance.proposal.support.AbstractIntegrationTest;
import com.team01.freelance.proposal.support.ProposalTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [S3-F8] Integration tests for {@code POST /api/proposals/{proposalId}/milestones}.
 */
@Transactional
class ProposalAddMilestonesIntegrationTest extends AbstractIntegrationTest {

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
    void milestoneSpecScenarioAddsMilestonesInOrderAndRejectsInvalidCases() throws Exception {
        Proposal submitted = saveProposal(ProposalStatus.SUBMITTED, 2000.0);

        mockMvc.perform(post("/api/proposals/{proposalId}/milestones", submitted.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                  {"title":"Planning","description":"Plan the work","amount":800.0},
                                  {"title":"Build","description":"Build the work","amount":700.0}
                                ]
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proposalMilestones", hasSize(2)))
                .andExpect(jsonPath("$.proposalMilestones[0].milestoneOrder").value(1))
                .andExpect(jsonPath("$.proposalMilestones[0].status").value("PENDING"))
                .andExpect(jsonPath("$.proposalMilestones[1].milestoneOrder").value(2))
                .andExpect(jsonPath("$.proposalMilestones[1].status").value("PENDING"));

        mockMvc.perform(post("/api/proposals/{proposalId}/milestones", submitted.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                  {"title":"Launch","description":"Launch the work","amount":500.0}
                                ]
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proposalMilestones", hasSize(3)))
                .andExpect(jsonPath("$.proposalMilestones[2].milestoneOrder").value(3))
                .andExpect(jsonPath("$.proposalMilestones[2].status").value("PENDING"));

        mockMvc.perform(post("/api/proposals/{proposalId}/milestones", submitted.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                  {"title":"Extra","description":"Extra work","amount":100.0}
                                ]
                                """))
                .andExpect(status().isBadRequest());

        Proposal accepted = saveProposal(ProposalStatus.ACCEPTED, 2000.0);
        mockMvc.perform(post("/api/proposals/{proposalId}/milestones", accepted.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                  {"title":"Accepted","description":"Accepted proposal milestone","amount":100.0}
                                ]
                                """))
                .andExpect(status().isBadRequest());

        Proposal invalidPayloadProposal = saveProposal(ProposalStatus.SUBMITTED, 2000.0);
        mockMvc.perform(post("/api/proposals/{proposalId}/milestones", invalidPayloadProposal.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                  {"description":"Missing title","amount":100.0}
                                ]
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingProposalReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/proposals/{proposalId}/milestones", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                  {"title":"Planning","description":"Plan","amount":100.0}
                                ]
                                """))
                .andExpect(status().isNotFound());
    }

    private Proposal saveProposal(ProposalStatus status, Double bidAmount) {
        Proposal proposal = new Proposal();
        proposal.setJobId(ProposalTestData.JOB_ID);
        proposal.setFreelancerId(ProposalTestData.FREELANCER_ID);
        proposal.setCoverLetter("Letter");
        proposal.setBidAmount(bidAmount);
        proposal.setEstimatedDays(7);
        proposal.setStatus(status);
        return proposalRepository.save(proposal);
    }
}
