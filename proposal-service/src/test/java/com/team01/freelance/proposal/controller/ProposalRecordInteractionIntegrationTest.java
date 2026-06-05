package com.team01.freelance.proposal.controller;

import com.team01.freelance.job.model.Job;
import com.team01.freelance.job.model.JobCategory;
import com.team01.freelance.job.model.JobStatus;
import com.team01.freelance.job.repository.JobRepository;
import com.team01.freelance.proposal.graph.InMemoryInteractionGraphService;
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
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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

    @Autowired
    private InMemoryInteractionGraphService interactionGraphService;

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
        job.setTitle("Interaction job");
        job.setDescription("Desc");
        job.setCategory(JobCategory.WEB_DEV);
        job.setStatus(JobStatus.OPEN);
        job.setBudgetMin(100.0);
        job.setBudgetMax(10000.0);
        job = jobRepository.save(job);

        freelancer = new User();
        freelancer.setName("Interaction Freelancer");
        freelancer.setEmail("interaction-" + System.nanoTime() + "@test.dev");
        freelancer.setPassword("secret");
        freelancer.setPhone("+8000" + (System.nanoTime() % 1_000_000_000L));
        freelancer.setRole(UserRole.FREELANCER);
        freelancer.setStatus(UserStatus.ACTIVE);
        freelancer = userRepository.save(freelancer);
    }

    @Test
    void recordInteraction_firstCall_recordsEdge() throws Exception {
        Proposal proposal = saveSubmittedProposal();

        mockMvc.perform(post("/api/proposals/{proposalId}/record-interaction", proposal.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Interaction recorded successfully"));

        assertThat(interactionGraphService.recordedProposalCount(freelancer.getId(), job.getId()))
                .isEqualTo(1);
    }

    @Test
    void recordInteraction_sameProposalTwice_isIdempotent() throws Exception {
        Proposal proposal = saveSubmittedProposal();

        mockMvc.perform(post("/api/proposals/{proposalId}/record-interaction", proposal.getId()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/proposals/{proposalId}/record-interaction", proposal.getId()))
                .andExpect(status().isOk());

        assertThat(interactionGraphService.recordedProposalCount(freelancer.getId(), job.getId()))
                .isEqualTo(1);
    }

    @Test
    void recordInteraction_secondProposalOnSameJob_incrementsCount() throws Exception {
        Proposal first = saveSubmittedProposal();
        Proposal second = saveSubmittedProposal();

        mockMvc.perform(post("/api/proposals/{proposalId}/record-interaction", first.getId()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/proposals/{proposalId}/record-interaction", second.getId()))
                .andExpect(status().isOk());

        assertThat(interactionGraphService.recordedProposalCount(freelancer.getId(), job.getId()))
                .isEqualTo(2);
    }

    @Test
    void recordInteraction_nonSubmittedProposal_returns400() throws Exception {
        Proposal proposal = saveSubmittedProposal();
        proposal.setStatus(ProposalStatus.WITHDRAWN);
        proposalRepository.save(proposal);

        mockMvc.perform(post("/api/proposals/{proposalId}/record-interaction", proposal.getId()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recordInteraction_missingProposal_returns404() throws Exception {
        mockMvc.perform(post("/api/proposals/{proposalId}/record-interaction", 99999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithAnonymousUser
    void recordInteraction_withoutAuth_returns401() throws Exception {
        Proposal proposal = saveSubmittedProposal();

        mockMvc.perform(post("/api/proposals/{proposalId}/record-interaction", proposal.getId()))
                .andExpect(status().isUnauthorized());
    }

    private Proposal saveSubmittedProposal() {
        Proposal proposal = new Proposal();
        proposal.setJobId(job.getId());
        proposal.setFreelancerId(freelancer.getId());
        proposal.setCoverLetter("Interaction letter");
        proposal.setBidAmount(500.0);
        proposal.setEstimatedDays(7);
        proposal.setStatus(ProposalStatus.SUBMITTED);
        proposal.setSubmittedAt(LocalDateTime.of(2026, 3, 10, 9, 0));
        return proposalRepository.save(proposal);
    }
}
