package com.team01.freelance.proposal.controller;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.context.WebApplicationContext;

import com.team01.freelance.contract.model.Contract;
import com.team01.freelance.contract.model.ContractStatus;
import com.team01.freelance.contract.repository.ContractRepository;
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

/**
 * [S3-F2] Integration tests for {@code PUT /api/proposals/{proposalId}/accept}.
 */
class ProposalAcceptIntegrationTest extends AbstractIntegrationTest {

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
    private ContractRepository contractRepository;

    private User client;
    private User freelancer;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(webApplicationContext);

        contractRepository.deleteAll();
        proposalRepository.deleteAll();
        jobRepository.deleteAll();
        userRepository.deleteAll();

        long suffix = System.nanoTime();
        client = saveUser("Client", "client-" + suffix + "@test.dev", UserRole.CLIENT);
        freelancer = saveUser("Freelancer", "freelancer-" + suffix + "@test.dev", UserRole.FREELANCER);
    }

    @Test
    void acceptSubmittedProposal_createsContractAndUpdatesJob() throws Exception {
        Job job = saveOpenJob();
        Proposal proposal = saveProposal(job.getId(), freelancer.getId(), ProposalStatus.SUBMITTED, 2000.0);

        mockMvc.perform(put("/api/proposals/{id}/accept", proposal.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.acceptedAt").exists());

        Proposal updated = proposalRepository.findById(proposal.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ProposalStatus.ACCEPTED);
        assertThat(updated.getAcceptedAt()).isNotNull();
        assertThat(jobRepository.findById(job.getId()).orElseThrow().getStatus()).isEqualTo(JobStatus.IN_PROGRESS);

        Contract contract = contractRepository.findByProposalId(proposal.getId()).orElseThrow();
        assertThat(contract.getStatus()).isEqualTo(ContractStatus.ACTIVE);
        assertThat(contract.getAgreedAmount()).isEqualTo(2000.0);
        assertThat(contract.getJobId()).isEqualTo(job.getId());
        assertThat(contract.getFreelancerId()).isEqualTo(freelancer.getId());
        assertThat(contract.getClientId()).isEqualTo(client.getId());
        assertThat(contract.getStartDate()).isNotNull();
    }

    @Test
    void acceptAlreadyAcceptedProposal_returns400() throws Exception {
        Job job = saveOpenJob();
        Proposal proposal = saveProposal(job.getId(), freelancer.getId(), ProposalStatus.ACCEPTED, 2000.0);

        mockMvc.perform(put("/api/proposals/{id}/accept", proposal.getId()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void acceptProposalForNonFreelancer_returns400() throws Exception {
        Job job = saveOpenJob();
        Proposal proposal = saveProposal(job.getId(), client.getId(), ProposalStatus.SUBMITTED, 2000.0);

        mockMvc.perform(put("/api/proposals/{id}/accept", proposal.getId()))
                .andExpect(status().isBadRequest());

        assertThat(contractRepository.count()).isZero();
        assertThat(jobRepository.findById(job.getId()).orElseThrow().getStatus()).isEqualTo(JobStatus.OPEN);
    }

    @Test
    void acceptUnknownProposal_returns404() throws Exception {
        mockMvc.perform(put("/api/proposals/{id}/accept", 999_999L))
                .andExpect(status().isNotFound());
    }

    private User saveUser(String name, String email, UserRole role) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword("secret");
        user.setPhone("+3000" + (System.nanoTime() % 1_000_000_000L));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    private Job saveOpenJob() {
        Job job = new Job();
        job.setClientId(client.getId());
        job.setTitle("Accept proposal job");
        job.setDescription("Integration test job");
        job.setCategory(JobCategory.WEB_DEV);
        job.setStatus(JobStatus.OPEN);
        job.setBudgetMin(500.0);
        job.setBudgetMax(5000.0);
        return jobRepository.save(job);
    }

    private Proposal saveProposal(Long jobId, Long freelancerId, ProposalStatus status, double bidAmount) {
        Proposal proposal = new Proposal();
        proposal.setJobId(jobId);
        proposal.setFreelancerId(freelancerId);
        proposal.setCoverLetter("Ready to start");
        proposal.setBidAmount(bidAmount);
        proposal.setEstimatedDays(14);
        proposal.setStatus(status);
        return proposalRepository.save(proposal);
    }

}