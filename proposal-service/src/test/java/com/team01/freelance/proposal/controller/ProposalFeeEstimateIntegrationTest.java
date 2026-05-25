package com.team01.freelance.proposal.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.test.context.support.WithMockUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

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
 * [S3-F3] Integration tests for {@code POST /api/proposals/estimate}.
 */
@WithMockUser(roles = "ADMIN")
class ProposalFeeEstimateIntegrationTest extends AbstractIntegrationTest {

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

        User client = new User();
        client.setName("Client");
        client.setEmail("client-" + System.nanoTime() + "@test.dev");
        client.setPassword("secret");
        client.setPhone("+2000" + (System.nanoTime() % 1_000_000_000L));
        client.setRole(UserRole.CLIENT);
        client.setStatus(UserStatus.ACTIVE);
        client = userRepository.save(client);

        job = new Job();
        job.setClientId(client.getId());
        job.setTitle("Fee estimate job");
        job.setDescription("Integration test job");
        job.setCategory(JobCategory.WEB_DEV);
        job.setStatus(JobStatus.OPEN);
        job.setBudgetMin(500.0);
        job.setBudgetMax(5000.0);
        job = jobRepository.save(job);

        freelancer = new User();
        freelancer.setName("Freelancer");
        freelancer.setEmail("freelancer-" + System.nanoTime() + "@test.dev");
        freelancer.setPassword("secret");
        freelancer.setPhone("+3000" + (System.nanoTime() % 1_000_000_000L));
        freelancer.setRole(UserRole.FREELANCER);
        freelancer.setStatus(UserStatus.ACTIVE);
        freelancer = userRepository.save(freelancer);
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
        proposal.setJobId(job.getId());
        proposal.setFreelancerId(freelancer.getId());
        proposal.setCoverLetter("Competing bid");
        proposal.setBidAmount(bidAmount);
        proposal.setEstimatedDays(10);
        proposal.setStatus(ProposalStatus.SUBMITTED);
        proposalRepository.save(proposal);
    }
}