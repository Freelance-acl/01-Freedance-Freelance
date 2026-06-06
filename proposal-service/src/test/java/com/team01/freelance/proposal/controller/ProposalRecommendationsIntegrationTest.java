package com.team01.freelance.proposal.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.team01.freelance.job.model.Job;
import com.team01.freelance.job.model.JobCategory;
import com.team01.freelance.job.model.JobStatus;
import com.team01.freelance.job.repository.JobRepository;
import com.team01.freelance.proposal.graph.InteractionGraphService;
import com.team01.freelance.user.model.User;
import com.team01.freelance.user.model.UserRole;
import com.team01.freelance.user.repository.UserRepository;
import com.team01.freelance.user.service.JwtService;
import com.team01.freelance.user.support.TestAuthHelper;
import com.team01.freelance.user.support.UserTestFixtures;

import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProposalRecommendationsIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private InteractionGraphService interactionGraphService;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;
    private User freelancerA;
    private User freelancerB;
    private Job job1;
    private Job job2;
    private Job job3;
    private Job job4;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        jobRepository.deleteAll();
        userRepository.deleteAll();

        freelancerA = saveFreelancer("Freelancer A");
        freelancerB = saveFreelancer("Freelancer B");
        User freelancerC = saveFreelancer("Freelancer C");

        job1 = saveJob("J1");
        job2 = saveJob("J2");
        job3 = saveJob("J3");
        job4 = saveJob("J4");

        record(1L, freelancerA, job1);
        record(2L, freelancerA, job2);
        record(3L, freelancerB, job1);
        record(4L, freelancerB, job3);
        record(5L, freelancerC, job2);
        record(6L, freelancerC, job4);
    }

    @Test
    void aclScenarioA_ownToken_returnsJ3AndJ4NotJ1OrJ2() throws Exception {
        mockMvc.perform(get("/api/proposals/recommendations")
                        .param("freelancerId", freelancerA.getId().toString())
                        .header("Authorization", TestAuthHelper.bearer(jwtService.generateToken(freelancerA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].jobId", hasItem(job3.getId().intValue())))
                .andExpect(jsonPath("$[*].jobId", hasItem(job4.getId().intValue())))
                .andExpect(jsonPath("$[*].jobId", not(hasItem(job1.getId().intValue()))))
                .andExpect(jsonPath("$[*].jobId", not(hasItem(job2.getId().intValue()))));
    }

    @Test
    void aclScenarioB_otherFreelancerToken_returns403() throws Exception {
        mockMvc.perform(get("/api/proposals/recommendations")
                        .param("freelancerId", freelancerA.getId().toString())
                        .header("Authorization", TestAuthHelper.bearer(jwtService.generateToken(freelancerB))))
                .andExpect(status().isForbidden());
    }

    @Test
    void aclScenarioC_adminToken_returns200() throws Exception {
        User admin = UserTestFixtures.seedAdmin(userRepository);
        mockMvc.perform(get("/api/proposals/recommendations")
                        .param("freelancerId", freelancerA.getId().toString())
                        .header("Authorization", TestAuthHelper.bearer(jwtService.generateToken(admin))))
                .andExpect(status().isOk());
    }

    @Test
    void aclScenarioD_noInteractions_returnsEmptyList() throws Exception {
        User isolated = saveFreelancer("Isolated");
        mockMvc.perform(get("/api/proposals/recommendations")
                        .param("freelancerId", isolated.getId().toString())
                        .header("Authorization", TestAuthHelper.bearer(jwtService.generateToken(isolated))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void aclScenarioE_unknownFreelancerWithAdmin_returns404() throws Exception {
        User admin = UserTestFixtures.seedAdmin(userRepository);
        mockMvc.perform(get("/api/proposals/recommendations")
                        .param("freelancerId", "999")
                        .header("Authorization", TestAuthHelper.bearer(jwtService.generateToken(admin))))
                .andExpect(status().isNotFound());
    }

    @Test
    void aclScenarioF_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/proposals/recommendations")
                        .param("freelancerId", freelancerA.getId().toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void defaultLimit_capsAtFiveResults() throws Exception {
        User target = saveFreelancer("Target");
        User peer = saveFreelancer("Peer");
        Job shared = saveJob("Shared");
        record(100L, target, shared);
        for (int i = 0; i < 7; i++) {
            Job extra = saveJob("Extra-" + i);
            record(200L + i, peer, shared);
            record(300L + i, peer, extra);
        }

        mockMvc.perform(get("/api/proposals/recommendations")
                        .param("freelancerId", target.getId().toString())
                        .header("Authorization", TestAuthHelper.bearer(jwtService.generateToken(target))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));
    }

    private void record(Long proposalId, User freelancer, Job job) {
        interactionGraphService.recordInteraction(
                proposalId,
                freelancer.getId(),
                freelancer.getName(),
                job.getId(),
                job.getTitle(),
                job.getCategory().name());
    }

    private User saveFreelancer(String name) {
        return UserTestFixtures.saveUser(
                userRepository,
                name,
                name.toLowerCase().replace(' ', '-') + "-" + System.nanoTime() + "@test.dev",
                "+3000" + (System.nanoTime() % 1_000_000_000L),
                UserRole.FREELANCER,
                UserTestFixtures.SEED_PASSWORD);
    }

    private Job saveJob(String title) {
        Job job = new Job();
        job.setClientId(1L);
        job.setTitle(title);
        job.setDescription(title + " description");
        job.setCategory(JobCategory.WEB_DEV);
        job.setStatus(JobStatus.OPEN);
        job.setBudgetMin(100.0);
        job.setBudgetMax(1000.0);
        return jobRepository.save(job);
    }
}
