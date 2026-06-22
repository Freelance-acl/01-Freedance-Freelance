package com.team01.freelance.proposal.controller;

import com.team01.freelance.proposal.model.MilestoneStatus;
import com.team01.freelance.proposal.model.Proposal;
import com.team01.freelance.proposal.model.ProposalMilestone;
import com.team01.freelance.proposal.repository.ProposalMilestoneRepository;
import com.team01.freelance.proposal.repository.ProposalRepository;
import com.team01.freelance.proposal.support.AbstractIntegrationTest;
import com.team01.freelance.proposal.support.ProposalTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [S3-F9] Integration tests for {@code GET /api/proposals/{proposalId}/details}.
 */
class ProposalDetailsIntegrationTest extends AbstractIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ProposalRepository proposalRepository;

    @Autowired
    private ProposalMilestoneRepository proposalMilestoneRepository;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(webApplicationContext);
        proposalMilestoneRepository.deleteAll();
        proposalRepository.deleteAll();
    }

    @Test
    void detailsReturnsMilestonesOrderedAndCountsCompletedStatuses() throws Exception {
        Proposal proposal = saveProposal();
        saveMilestone(proposal, 3, "Final", MilestoneStatus.PENDING);
        saveMilestone(proposal, 1, "Start", MilestoneStatus.COMPLETED);
        saveMilestone(proposal, 2, "Review", MilestoneStatus.COMPLETED);

        mockMvc.perform(get("/api/proposals/{proposalId}/details", proposal.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proposalId").value(proposal.getId()))
                .andExpect(jsonPath("$.jobId").value(ProposalTestData.JOB_ID))
                .andExpect(jsonPath("$.freelancerId").value(ProposalTestData.FREELANCER_ID))
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.bidAmount").value(2000.0))
                .andExpect(jsonPath("$.totalMilestones").value(3))
                .andExpect(jsonPath("$.completedMilestones").value(2))
                .andExpect(jsonPath("$.milestones", hasSize(3)))
                .andExpect(jsonPath("$.milestones[0].milestoneOrder").value(1))
                .andExpect(jsonPath("$.milestones[0].title").value("Start"))
                .andExpect(jsonPath("$.milestones[1].milestoneOrder").value(2))
                .andExpect(jsonPath("$.milestones[1].title").value("Review"))
                .andExpect(jsonPath("$.milestones[2].milestoneOrder").value(3))
                .andExpect(jsonPath("$.milestones[2].title").value("Final"));
    }

    @Test
    void detailsCountsApprovedMilestonesAsCompleted() throws Exception {
        Proposal proposal = saveProposal();
        saveMilestone(proposal, 1, "Approved work", MilestoneStatus.APPROVED);

        mockMvc.perform(get("/api/proposals/{proposalId}/details", proposal.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMilestones").value(1))
                .andExpect(jsonPath("$.completedMilestones").value(1));
    }

    @Test
    void detailsReturnsEmptyMilestoneListWhenProposalHasNoMilestones() throws Exception {
        Proposal proposal = saveProposal();

        mockMvc.perform(get("/api/proposals/{proposalId}/details", proposal.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proposalId").value(proposal.getId()))
                .andExpect(jsonPath("$.totalMilestones").value(0))
                .andExpect(jsonPath("$.completedMilestones").value(0))
                .andExpect(jsonPath("$.milestones", hasSize(0)));
    }

    @Test
    void detailsReturnsNotFoundWhenProposalDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/proposals/{proposalId}/details", 999999L))
                .andExpect(status().isNotFound());
    }

    private Proposal saveProposal() {
        Proposal proposal = new Proposal();
        proposal.setJobId(ProposalTestData.JOB_ID);
        proposal.setFreelancerId(ProposalTestData.FREELANCER_ID);
        proposal.setCoverLetter("Details letter");
        proposal.setBidAmount(2000.0);
        proposal.setEstimatedDays(7);
        proposal.setStatus(com.team01.freelance.proposal.model.ProposalStatus.SUBMITTED);
        return proposalRepository.save(proposal);
    }

    private void saveMilestone(Proposal proposal, Integer milestoneOrder, String title, MilestoneStatus status) {
        ProposalMilestone milestone = new ProposalMilestone();
        milestone.setProposal(proposal);
        milestone.setMilestoneOrder(milestoneOrder);
        milestone.setTitle(title);
        milestone.setDescription(title + " description");
        milestone.setAmount(100.0);
        milestone.setStatus(status);
        proposalMilestoneRepository.save(milestone);
    }
}
