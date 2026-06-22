package com.team01.freelance.proposal.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team01.freelance.proposal.config.security.JwtService;
import com.team01.freelance.proposal.dto.FeignJobDTO;
import com.team01.freelance.proposal.dto.FeignUserDTO;
import com.team01.freelance.proposal.graph.InteractionGraphService;
import com.team01.freelance.proposal.support.FeignIntegrationTestSupport;
import com.team01.freelance.proposal.support.FeignTestFixtures;
import com.team01.freelance.proposal.support.ProposalJwtTestSupport;
import com.team01.freelance.proposal.support.ProposalTestData;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * [S3-F12] Integration tests for {@code GET /api/proposals/recommendations}.
 */
@Transactional
class ProposalRecommendationsIntegrationTest extends FeignIntegrationTestSupport {

    private static final long FREELANCER_A = 201L;
    private static final long FREELANCER_B = 202L;
    private static final long FREELANCER_C = 203L;
    private static final long ADMIN_ID = 1L;
    private static final long JOB_1 = 101L;
    private static final long JOB_2 = 102L;
    private static final long JOB_3 = 103L;
    private static final long JOB_4 = 104L;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private InteractionGraphService interactionGraphService;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;
    private final Map<Long, FeignJobDTO> jobs = new HashMap<>();

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(webApplicationContext);
        jobs.clear();

        when(jobServiceClient.getJob(anyLong())).thenAnswer(invocation -> {
            Long jobId = invocation.getArgument(0);
            FeignJobDTO job = jobs.get(jobId);
            if (job == null) {
                throw notFound();
            }
            return job;
        });

        stubFreelancer(FREELANCER_A, "Freelancer A");
        stubFreelancer(FREELANCER_B, "Freelancer B");
        stubFreelancer(FREELANCER_C, "Freelancer C");

        stubJob(JOB_1, "J1");
        stubJob(JOB_2, "J2");
        stubJob(JOB_3, "J3");
        stubJob(JOB_4, "J4");

        record(1L, FREELANCER_A, "Freelancer A", JOB_1, "J1");
        record(2L, FREELANCER_A, "Freelancer A", JOB_2, "J2");
        record(3L, FREELANCER_B, "Freelancer B", JOB_1, "J1");
        record(4L, FREELANCER_B, "Freelancer B", JOB_3, "J3");
        record(5L, FREELANCER_C, "Freelancer C", JOB_2, "J2");
        record(6L, FREELANCER_C, "Freelancer C", JOB_4, "J4");
    }

    @Test
    void aclScenarioA_ownToken_returnsJ3AndJ4NotJ1OrJ2() throws Exception {
        String token = ProposalJwtTestSupport.token(
                jwtService, "freelancer-a@test.dev", FREELANCER_A, "FREELANCER");

        mockMvc.perform(get("/api/proposals/recommendations")
                        .param("freelancerId", Long.toString(FREELANCER_A))
                        .header("Authorization", ProposalJwtTestSupport.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].jobId", hasItem((int) JOB_3)))
                .andExpect(jsonPath("$[*].jobId", hasItem((int) JOB_4)))
                .andExpect(jsonPath("$[*].jobId", not(hasItem((int) JOB_1))))
                .andExpect(jsonPath("$[*].jobId", not(hasItem((int) JOB_2))));
    }

    @Test
    void aclScenarioB_otherFreelancerToken_returns403() throws Exception {
        String token = ProposalJwtTestSupport.token(
                jwtService, "freelancer-b@test.dev", FREELANCER_B, "FREELANCER");

        mockMvc.perform(get("/api/proposals/recommendations")
                        .param("freelancerId", Long.toString(FREELANCER_A))
                        .header("Authorization", ProposalJwtTestSupport.bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void aclScenarioC_adminToken_returns200() throws Exception {
        stubAdmin();
        String token = ProposalJwtTestSupport.token(jwtService, "admin@test.dev", ADMIN_ID, "ADMIN");

        mockMvc.perform(get("/api/proposals/recommendations")
                        .param("freelancerId", Long.toString(FREELANCER_A))
                        .header("Authorization", ProposalJwtTestSupport.bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void aclScenarioD_noInteractions_returnsEmptyList() throws Exception {
        long isolated = 299L;
        stubFreelancer(isolated, "Isolated");
        String token = ProposalJwtTestSupport.token(
                jwtService, "isolated@test.dev", isolated, "FREELANCER");

        mockMvc.perform(get("/api/proposals/recommendations")
                        .param("freelancerId", Long.toString(isolated))
                        .header("Authorization", ProposalJwtTestSupport.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void aclScenarioE_unknownFreelancerWithAdmin_returns404() throws Exception {
        stubAdmin();
        when(userServiceClient.getUser(999L)).thenThrow(notFound());
        String token = ProposalJwtTestSupport.token(jwtService, "admin@test.dev", ADMIN_ID, "ADMIN");

        mockMvc.perform(get("/api/proposals/recommendations")
                        .param("freelancerId", "999")
                        .header("Authorization", ProposalJwtTestSupport.bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void aclScenarioF_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/proposals/recommendations")
                        .param("freelancerId", Long.toString(FREELANCER_A)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void defaultLimit_capsAtFiveResults() throws Exception {
        long target = 250L;
        long peer = 251L;
        long shared = 150L;
        stubFreelancer(target, "Target");
        stubFreelancer(peer, "Peer");
        stubJob(shared, "Shared");

        record(100L, target, "Target", shared, "Shared");
        for (int i = 0; i < 7; i++) {
            long extraJob = 160L + i;
            stubJob(extraJob, "Extra-" + i);
            record(200L + i, peer, "Peer", shared, "Shared");
            record(300L + i, peer, "Peer", extraJob, "Extra-" + i);
        }

        String token = ProposalJwtTestSupport.token(
                jwtService, "target@test.dev", target, "FREELANCER");

        mockMvc.perform(get("/api/proposals/recommendations")
                        .param("freelancerId", Long.toString(target))
                        .header("Authorization", ProposalJwtTestSupport.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));
    }

    private void record(Long proposalId, long freelancerId, String freelancerName,
                        long jobId, String jobTitle) {
        interactionGraphService.recordInteraction(
                proposalId,
                freelancerId,
                freelancerName,
                jobId,
                jobTitle,
                "WEB_DEV");
    }

    private void stubFreelancer(long id, String name) {
        FeignUserDTO user = FeignTestFixtures.activeFreelancer(id);
        user.setName(name);
        when(userServiceClient.getUser(id)).thenReturn(user);
    }

    private void stubAdmin() {
        FeignUserDTO admin = FeignTestFixtures.activeClient(ADMIN_ID);
        admin.setName("Admin");
        admin.setRole("ADMIN");
        when(userServiceClient.getUser(ADMIN_ID)).thenReturn(admin);
    }

    private void stubJob(long id, String title) {
        FeignJobDTO job = FeignTestFixtures.openJob(id, ProposalTestData.CLIENT_ID);
        job.setTitle(title);
        jobs.put(id, job);
    }

    private static FeignException.NotFound notFound() {
        Request request = Request.create(
                Request.HttpMethod.GET, "/test", Collections.emptyMap(), null, new RequestTemplate());
        return new FeignException.NotFound("not found", request, null, null);
    }
}
