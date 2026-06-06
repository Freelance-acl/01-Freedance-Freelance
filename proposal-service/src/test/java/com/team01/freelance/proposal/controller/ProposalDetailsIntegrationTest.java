package com.team01.freelance.proposal.controller;

import com.team01.freelance.job.model.Job;
import com.team01.freelance.job.model.JobCategory;
import com.team01.freelance.job.model.JobStatus;
import com.team01.freelance.job.repository.JobRepository;
import com.team01.freelance.proposal.model.MilestoneStatus;
import com.team01.freelance.proposal.model.Proposal;
import com.team01.freelance.proposal.model.ProposalMilestone;
import com.team01.freelance.proposal.model.ProposalStatus;
import com.team01.freelance.proposal.repository.ProposalMilestoneRepository;
import com.team01.freelance.proposal.repository.ProposalRepository;
import com.team01.freelance.proposal.support.AbstractIntegrationTest;
import com.team01.freelance.user.model.User;
import com.team01.freelance.user.model.UserRole;
import com.team01.freelance.user.model.UserStatus;
import com.team01.freelance.user.repository.UserRepository;
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

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

    private Job job;
    private User freelancer;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(webApplicationContext);

        proposalMilestoneRepository.deleteAll();
        proposalRepository.deleteAll();
        jobRepository.deleteAll();
        userRepository.deleteAll();

        job = new Job();
        job.setClientId(1L);
        job.setTitle("Proposal details job");
        job.setDescription("Details job");
        job.setCategory(JobCategory.WEB_DEV);
        job.setStatus(JobStatus.OPEN);
        job.setBudgetMin(100.0);
        job.setBudgetMax(5000.0);
        job = jobRepository.save(job);

        freelancer = new User();
        freelancer.setName("Details Freelancer");
        freelancer.setEmail("details-" + System.nanoTime() + "@test.dev");
        freelancer.setPassword("secret");
        freelancer.setPhone("+4000" + (System.nanoTime() % 1_000_000_000L));
        freelancer.setRole(UserRole.FREELANCER);
        freelancer.setStatus(UserStatus.ACTIVE);
        freelancer = userRepository.save(freelancer);
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
                .andExpect(jsonPath("$.jobId").value(job.getId()))
                .andExpect(jsonPath("$.freelancerId").value(freelancer.getId()))
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
        proposal.setJobId(job.getId());
        proposal.setFreelancerId(freelancer.getId());
        proposal.setCoverLetter("Details letter");
        proposal.setBidAmount(2000.0);
        proposal.setEstimatedDays(7);
        proposal.setStatus(ProposalStatus.SUBMITTED);
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