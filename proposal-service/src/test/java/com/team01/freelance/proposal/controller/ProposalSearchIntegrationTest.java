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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [S3-F1] Integration tests for {@code GET /api/proposals/search}.
 */
class ProposalSearchIntegrationTest extends AbstractIntegrationTest {

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
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        proposalRepository.deleteAll();
        jobRepository.deleteAll();
        userRepository.deleteAll();

        job = new Job();
        job.setClientId(1L);
        job.setTitle("Integration job");
        job.setDescription("Desc");
        job.setCategory(JobCategory.WEB_DEV);
        job.setStatus(JobStatus.OPEN);
        job.setBudgetMin(100.0);
        job.setBudgetMax(500.0);
        job = jobRepository.save(job);

        freelancer = new User();
        freelancer.setName("Freelancer One");
        freelancer.setEmail("fl1-" + System.nanoTime() + "@test.dev");
        freelancer.setPassword("secret");
        freelancer.setPhone("+1000" + (System.nanoTime() % 1_000_000_000L));
        freelancer.setRole(UserRole.FREELANCER);
        freelancer.setStatus(UserStatus.ACTIVE);
        freelancer = userRepository.save(freelancer);
    }

    @Test
    void milestoneSpecScenario_searchByAcceptedInMarch_returnsTwoNewestFirst() throws Exception {
        LocalDateTime marchOlder = LocalDateTime.of(2026, 3, 5, 10, 0);
        LocalDateTime marchNewer = LocalDateTime.of(2026, 3, 20, 15, 0);
        LocalDateTime marchSubmitted = LocalDateTime.of(2026, 3, 10, 11, 0);
        LocalDateTime feb1 = LocalDateTime.of(2026, 2, 1, 9, 0);
        LocalDateTime feb2 = LocalDateTime.of(2026, 2, 15, 9, 0);

        Proposal pMarchAcc1 = saveProposal(ProposalStatus.ACCEPTED, marchOlder);
        Proposal pMarchAcc2 = saveProposal(ProposalStatus.ACCEPTED, marchNewer);
        saveProposal(ProposalStatus.SUBMITTED, marchSubmitted);
        saveProposal(ProposalStatus.ACCEPTED, feb1);
        saveProposal(ProposalStatus.ACCEPTED, feb2);

        mockMvc.perform(get("/api/proposals/search")
                        .param("status", "ACCEPTED")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(pMarchAcc2.getId().intValue()))
                .andExpect(jsonPath("$[1].id").value(pMarchAcc1.getId().intValue()));
    }

    @Test
    void milestoneSpecScenario_searchMarchWithoutStatus_returnsThreeNewestFirst() throws Exception {
        LocalDateTime marchOlder = LocalDateTime.of(2026, 3, 5, 10, 0);
        LocalDateTime marchNewer = LocalDateTime.of(2026, 3, 20, 15, 0);
        LocalDateTime marchSubmitted = LocalDateTime.of(2026, 3, 10, 11, 0);
        Proposal pMarchOldAcc = saveProposal(ProposalStatus.ACCEPTED, marchOlder);
        Proposal pMarchNewAcc = saveProposal(ProposalStatus.ACCEPTED, marchNewer);
        Proposal pSubmitted = saveProposal(ProposalStatus.SUBMITTED, marchSubmitted);
        saveProposal(ProposalStatus.ACCEPTED, LocalDateTime.of(2026, 2, 1, 9, 0));
        saveProposal(ProposalStatus.ACCEPTED, LocalDateTime.of(2026, 2, 15, 9, 0));

        mockMvc.perform(get("/api/proposals/search")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id").value(pMarchNewAcc.getId().intValue()))
                .andExpect(jsonPath("$[1].id").value(pSubmitted.getId().intValue()))
                .andExpect(jsonPath("$[2].id").value(pMarchOldAcc.getId().intValue()));
    }

    @Test
    void inclusiveEndDate_includesProposalsOnLastDayOfRange() throws Exception {
        LocalDateTime endDayMorning = LocalDateTime.of(2026, 7, 31, 23, 59, 59);
        Proposal p = saveProposal(ProposalStatus.SUBMITTED, endDayMorning);

        mockMvc.perform(get("/api/proposals/search")
                        .param("startDate", "2026-07-01")
                        .param("endDate", "2026-07-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(p.getId().intValue()));
    }

    @Test
    void invalidStatus_returns400() throws Exception {
        mockMvc.perform(get("/api/proposals/search")
                        .param("status", "UNKNOWN")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-01-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid ProposalStatus")));
    }

    @Test
    void startDateAfterEndDate_returns400() throws Exception {
        mockMvc.perform(get("/api/proposals/search")
                        .param("startDate", "2026-02-10")
                        .param("endDate", "2026-02-01"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message", containsString("startDate")));
    }

    @Test
    void sameDayRange_returnsMatchesThatDay() throws Exception {
        LocalDateTime noon = LocalDateTime.of(2026, 8, 15, 12, 0);
        Proposal p = saveProposal(ProposalStatus.REJECTED, noon);

        mockMvc.perform(get("/api/proposals/search")
                        .param("startDate", "2026-08-15")
                        .param("endDate", "2026-08-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(p.getId().intValue()));
    }

    private Proposal saveProposal(ProposalStatus status, LocalDateTime submittedAt) {
        Proposal proposal = new Proposal();
        proposal.setJobId(job.getId());
        proposal.setFreelancerId(freelancer.getId());
        proposal.setCoverLetter("Letter");
        proposal.setBidAmount(250.0);
        proposal.setEstimatedDays(7);
        proposal.setStatus(status);
        proposal.setSubmittedAt(submittedAt);
        return proposalRepository.save(proposal);
    }
}
