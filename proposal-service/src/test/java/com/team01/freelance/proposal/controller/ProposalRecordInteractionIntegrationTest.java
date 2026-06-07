package com.team01.freelance.proposal.controller;

import com.team01.freelance.job.model.Job;
import com.team01.freelance.job.model.JobCategory;
import com.team01.freelance.job.model.JobStatus;
import com.team01.freelance.job.repository.JobRepository;
import com.team01.freelance.proposal.model.Proposal;
import com.team01.freelance.proposal.model.ProposalStatus;
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

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [S3-F11] Integration tests for {@code POST /api/proposals/{proposalId}/record-interaction}.
 */
class ProposalRecordInteractionIntegrationTest extends AbstractIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ProposalRepository proposalRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

    private Job job;
    private User freelancer;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(webApplicationContext);

        proposalRepository.deleteAll();
        jobRepository.deleteAll();
        userRepository.deleteAll();

        job = new Job();
        job.setClientId(1L);
        job.setTitle("Interaction Job");
        job.setDescription("Desc");
        job.setCategory(JobCategory.WEB_DEV);
        job.setStatus(JobStatus.OPEN);
        job.setBudgetMin(100.0);
        job.setBudgetMax(5000.0);
        job = jobRepository.save(job);

        freelancer = new User();
        freelancer.setName("Interaction Freelancer");
        freelancer.setEmail("interact-" + System.nanoTime() + "@test.dev");
        freelancer.setPassword("secret");
        freelancer.setPhone("+8000" + (System.nanoTime() % 1_000_000_000L));
        freelancer.setRole(UserRole.FREELANCER);
        freelancer.setStatus(UserStatus.ACTIVE);
        freelancer = userRepository.save(freelancer);
    }

    @Test
    void recordInteraction_submittedProposal_returns200() throws Exception {
        Proposal proposal = saveProposal(ProposalStatus.SUBMITTED);

        mockMvc.perform(post("/api/proposals/{proposalId}/record-interaction", proposal.getId()))
                .andExpect(status().isOk());
    }

    @Test
    void recordInteraction_isIdempotent_secondCallStillReturns200() throws Exception {
        Proposal proposal = saveProposal(ProposalStatus.SUBMITTED);

        mockMvc.perform(post("/api/proposals/{proposalId}/record-interaction", proposal.getId()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/proposals/{proposalId}/record-interaction", proposal.getId()))
                .andExpect(status().isOk());
    }

    @Test
    void recordInteraction_nonSubmittedProposal_returns400() throws Exception {
        Proposal proposal = saveProposal(ProposalStatus.ACCEPTED);

        mockMvc.perform(post("/api/proposals/{proposalId}/record-interaction", proposal.getId()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recordInteraction_nonExistentProposal_returns404() throws Exception {
        mockMvc.perform(post("/api/proposals/{proposalId}/record-interaction", 999999L))
                .andExpect(status().isNotFound());
    }

    private Proposal saveProposal(ProposalStatus status) {
        Proposal proposal = new Proposal();
        proposal.setJobId(job.getId());
        proposal.setFreelancerId(freelancer.getId());
        proposal.setCoverLetter("Cover letter");
        proposal.setBidAmount(500.0);
        proposal.setEstimatedDays(7);
        proposal.setStatus(status);
        proposal.setSubmittedAt(LocalDateTime.now());
        return proposalRepository.save(proposal);
    }
}
