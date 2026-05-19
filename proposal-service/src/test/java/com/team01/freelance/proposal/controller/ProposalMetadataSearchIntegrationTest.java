package com.team01.freelance.proposal.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
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
 * [S3-F5] Integration tests for {@code GET /api/proposals/metadata/search}.
 */
@Transactional
class ProposalMetadataSearchIntegrationTest extends AbstractIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ProposalRepository proposalRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

    private User client;
    private User freelancer;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        long suffix = System.nanoTime();
        client = saveUser("Client", "client-" + suffix + "@test.dev", UserRole.CLIENT);
        freelancer = saveUser("Freelancer", "freelancer-" + suffix + "@test.dev", UserRole.FREELANCER);
    }

    @Test
    void searchByMetadata_returnsMatchingProposals() throws Exception {
        Job job1 = saveJob();
        Job job2 = saveJob();
        Job job3 = saveJob();

        Proposal agile = saveProposal(job1.getId(), Map.of("approach", "agile"));
        saveProposal(job2.getId(), Map.of("approach", "waterfall"));
        saveProposal(job3.getId(), Map.of("approach", "waterfall"));

        mockMvc.perform(get("/api/proposals/metadata/search")
                        .param("key", "approach")
                        .param("value", "agile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(agile.getId().intValue()))
                .andExpect(jsonPath("$[0].metadata.approach").value("agile"));
    }

    @Test
    void searchByMetadata_blankKey_returns400() throws Exception {
        mockMvc.perform(get("/api/proposals/metadata/search")
                        .param("key", "")
                        .param("value", "x"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchByMetadata_blankValue_returns400() throws Exception {
        mockMvc.perform(get("/api/proposals/metadata/search")
                        .param("key", "approach")
                        .param("value", "   "))
                .andExpect(status().isBadRequest());
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

    private Job saveJob() {
        Job job = new Job();
        job.setClientId(client.getId());
        job.setTitle("Metadata search job");
        job.setDescription("Integration test job");
        job.setCategory(JobCategory.WEB_DEV);
        job.setStatus(JobStatus.OPEN);
        job.setBudgetMin(500.0);
        job.setBudgetMax(5000.0);
        return jobRepository.save(job);
    }

    private Proposal saveProposal(Long jobId, Map<String, Object> metadata) {
        Map<String, Object> storedMetadata = new LinkedHashMap<>(metadata);
        Proposal proposal = new Proposal();
        proposal.setJobId(jobId);
        proposal.setFreelancerId(freelancer.getId());
        proposal.setCoverLetter("Metadata test proposal");
        proposal.setBidAmount(2000.0);
        proposal.setEstimatedDays(14);
        proposal.setStatus(ProposalStatus.SUBMITTED);
        proposal.setMetadata(storedMetadata);
        return proposalRepository.save(proposal);
    }
}
