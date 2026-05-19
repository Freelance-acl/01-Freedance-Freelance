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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class ProposalWithdrawIntegrationTest extends AbstractIntegrationTest {

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
    private JdbcTemplate jdbcTemplate;

    private User freelancer;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        proposalRepository.deleteAll();
        jobRepository.deleteAll();
        userRepository.deleteAll();

        freelancer = new User();
        freelancer.setName("Withdraw Freelancer");
        freelancer.setEmail("withdraw-" + System.nanoTime() + "@test.dev");
        freelancer.setPassword("secret");
        freelancer.setPhone("+2000" + (System.nanoTime() % 1_000_000_000L));
        freelancer.setRole(UserRole.FREELANCER);
        freelancer.setStatus(UserStatus.ACTIVE);
        freelancer = userRepository.save(freelancer);
    }

    @Test
    void withdrawSubmittedProposalForOpenJobSetsProposalWithdrawn() throws Exception {
        Job job = saveJob(JobStatus.OPEN);
        Proposal proposal = saveProposal(job.getId(), ProposalStatus.SUBMITTED);

        mockMvc.perform(put("/api/proposals/{id}/withdraw", proposal.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WITHDRAWN"));

        assertThat(proposalRepository.findById(proposal.getId()).orElseThrow().getStatus())
                .isEqualTo(ProposalStatus.WITHDRAWN);
        assertThat(jobStatusFromDatabase(job.getId())).isEqualTo("OPEN");
    }

    @Test
    void withdrawOnlyActiveProposalForInProgressJobReopensJob() throws Exception {
        Job job = saveJob(JobStatus.IN_PROGRESS);
        Proposal proposal = saveProposal(job.getId(), ProposalStatus.SUBMITTED);

        mockMvc.perform(put("/api/proposals/{id}/withdraw", proposal.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WITHDRAWN"));

        assertThat(jobStatusFromDatabase(job.getId())).isEqualTo("OPEN");
    }

    @Test
    void withdrawAcceptedProposalReturnsBadRequest() throws Exception {
        Job job = saveJob(JobStatus.OPEN);
        Proposal proposal = saveProposal(job.getId(), ProposalStatus.ACCEPTED);

        mockMvc.perform(put("/api/proposals/{id}/withdraw", proposal.getId()))
                .andExpect(status().isBadRequest());

        assertThat(proposalRepository.findById(proposal.getId()).orElseThrow().getStatus())
                .isEqualTo(ProposalStatus.ACCEPTED);
    }

    @Test
    void withdrawRejectedProposalReturnsBadRequest() throws Exception {
        Job job = saveJob(JobStatus.OPEN);
        Proposal proposal = saveProposal(job.getId(), ProposalStatus.REJECTED);

        mockMvc.perform(put("/api/proposals/{id}/withdraw", proposal.getId()))
                .andExpect(status().isBadRequest());

        assertThat(proposalRepository.findById(proposal.getId()).orElseThrow().getStatus())
                .isEqualTo(ProposalStatus.REJECTED);
    }

    @Test
    void withdrawMissingProposalReturnsNotFound() throws Exception {
        mockMvc.perform(put("/api/proposals/{id}/withdraw", 999999L))
                .andExpect(status().isNotFound());
    }

    private Job saveJob(JobStatus status) {
        Job job = new Job();
        job.setClientId(1L);
        job.setTitle("Withdraw job");
        job.setDescription("Desc");
        job.setCategory(JobCategory.WEB_DEV);
        job.setStatus(status);
        job.setBudgetMin(100.0);
        job.setBudgetMax(500.0);
        return jobRepository.save(job);
    }

    private Proposal saveProposal(Long jobId, ProposalStatus status) {
        Proposal proposal = new Proposal();
        proposal.setJobId(jobId);
        proposal.setFreelancerId(freelancer.getId());
        proposal.setCoverLetter("Please consider me");
        proposal.setBidAmount(250.0);
        proposal.setEstimatedDays(7);
        proposal.setStatus(status);
        return proposalRepository.save(proposal);
    }

    private String jobStatusFromDatabase(Long jobId) {
        return jdbcTemplate.queryForObject("SELECT status FROM jobs WHERE id = ?", String.class, jobId);
    }
}
