package com.team01.freelance.proposal.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.test.context.support.WithMockUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
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
import com.team01.freelance.wallet.model.Payout;
import com.team01.freelance.wallet.model.PayoutStatus;
import com.team01.freelance.wallet.repository.PayoutRepository;

/**
 * [S3-F4] Integration tests for {@code PUT /api/proposals/{id}/complete}.
 */
@WithMockUser(roles = "ADMIN")
class ProposalCompleteIntegrationTest extends AbstractIntegrationTest {

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

    @Autowired
    private PayoutRepository payoutRepository;

    private User client;
    private User freelancer;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(webApplicationContext);

        payoutRepository.deleteAll();
        contractRepository.deleteAll();
        proposalRepository.deleteAll();
        jobRepository.deleteAll();
        userRepository.deleteAll();

        long suffix = System.nanoTime();
        client = saveUser("Client", "client-" + suffix + "@test.dev", UserRole.CLIENT);
        freelancer = saveUser("Freelancer", "freelancer-" + suffix + "@test.dev", UserRole.FREELANCER);
    }

    @Test
    void completeAcceptedProposal_closesContractJobAndCreatesPayout() throws Exception {
        AcceptedWorkSetup setup = saveAcceptedWork(2000.0);

        mockMvc.perform(put("/api/proposals/{id}/complete", setup.proposal().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        Contract contract = contractRepository.findByProposalId(setup.proposal().getId()).orElseThrow();
        assertThat(contract.getStatus()).isEqualTo(ContractStatus.COMPLETED);
        assertThat(contract.getEndDate()).isNotNull();
        assertThat(jobRepository.findById(setup.job().getId()).orElseThrow().getStatus()).isEqualTo(JobStatus.CLOSED);

        Payout payout = payoutRepository.findByContractIdAndStatus(
                contract.getId(), PayoutStatus.PENDING).orElseThrow();
        assertThat(payout.getAmount()).isEqualTo(2000.0);
        assertThat(payout.getFreelancerId()).isEqualTo(freelancer.getId());
    }

    @Test
    void completeAgain_returns400WhenNoActiveContract() throws Exception {
        AcceptedWorkSetup setup = saveAcceptedWork(2000.0);

        mockMvc.perform(put("/api/proposals/{id}/complete", setup.proposal().getId()))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/proposals/{id}/complete", setup.proposal().getId()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void completeSubmittedProposal_returns400() throws Exception {
        Job job = saveJob(JobStatus.IN_PROGRESS);
        Proposal proposal = saveProposal(job.getId(), freelancer.getId(), ProposalStatus.SUBMITTED, 2000.0);

        mockMvc.perform(put("/api/proposals/{id}/complete", proposal.getId()))
                .andExpect(status().isBadRequest());

        assertThat(payoutRepository.count()).isZero();
    }

    private AcceptedWorkSetup saveAcceptedWork(double agreedAmount) {
        Job job = saveJob(JobStatus.IN_PROGRESS);
        Proposal proposal = saveProposal(job.getId(), freelancer.getId(), ProposalStatus.ACCEPTED, agreedAmount);
        LocalDateTime now = LocalDateTime.now();

        Contract contract = new Contract();
        contract.setJobId(job.getId());
        contract.setFreelancerId(freelancer.getId());
        contract.setClientId(client.getId());
        contract.setProposalId(proposal.getId());
        contract.setAgreedAmount(agreedAmount);
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setStartDate(now);
        contract.setCreatedAt(now);
        contractRepository.save(contract);

        return new AcceptedWorkSetup(job, proposal);
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

    private Job saveJob(JobStatus status) {
        Job job = new Job();
        job.setClientId(client.getId());
        job.setTitle("Complete proposal job");
        job.setDescription("Integration test job");
        job.setCategory(JobCategory.WEB_DEV);
        job.setStatus(status);
        job.setBudgetMin(500.0);
        job.setBudgetMax(5000.0);
        return jobRepository.save(job);
    }

    private Proposal saveProposal(Long jobId, Long freelancerId, ProposalStatus status, double bidAmount) {
        Proposal proposal = new Proposal();
        proposal.setJobId(jobId);
        proposal.setFreelancerId(freelancerId);
        proposal.setCoverLetter("Ready to complete");
        proposal.setBidAmount(bidAmount);
        proposal.setEstimatedDays(14);
        proposal.setStatus(status);
        if (status == ProposalStatus.ACCEPTED) {
            proposal.setAcceptedAt(LocalDateTime.now());
        }
        return proposalRepository.save(proposal);
    }

    private record AcceptedWorkSetup(Job job, Proposal proposal) {
    }
}