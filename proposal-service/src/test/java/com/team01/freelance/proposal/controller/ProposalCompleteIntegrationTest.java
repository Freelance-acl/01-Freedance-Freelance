package com.team01.freelance.proposal.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team01.freelance.proposal.dto.FeignContractDTO;
import com.team01.freelance.proposal.dto.FeignJobDTO;
import com.team01.freelance.proposal.dto.FeignUserDTO;
import com.team01.freelance.proposal.messaging.ProposalEventPublisher;
import com.team01.freelance.proposal.model.Proposal;
import com.team01.freelance.proposal.model.ProposalStatus;
import com.team01.freelance.proposal.repository.ProposalRepository;
import com.team01.freelance.proposal.saga.SagaTriggerService;
import com.team01.freelance.proposal.support.FeignIntegrationTestSupport;
import com.team01.freelance.proposal.support.FeignTestFixtures;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

/**
 * [S3-F4] Integration tests for {@code PUT /api/proposals/{id}/complete}.
 */
class ProposalCompleteIntegrationTest extends FeignIntegrationTestSupport {

    private static final long CLIENT_ID = 1L;
    private static final long FREELANCER_ID = 200L;
    private static final long JOB_ID = 100L;

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ProposalRepository proposalRepository;

    @Autowired
    private SagaTriggerService sagaTriggerService;

    private ProposalEventPublisher proposalEventPublisher;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(webApplicationContext);
        proposalEventPublisher = org.mockito.Mockito.mock(ProposalEventPublisher.class);
        ReflectionTestUtils.setField(sagaTriggerService, "proposalEventPublisher", proposalEventPublisher);
        proposalRepository.deleteAll();
    }

    @Test
    void completeAcceptedProposal_transitionsToCompletingAndPublishesEvent() throws Exception {
        Proposal proposal = saveAcceptedProposal(2000.0);
        FeignContractDTO contract = FeignTestFixtures.activeContract(900L, proposal.getId(), 2000.0);
        stubHappyPathPreChecks(contract);

        mockMvc.perform(put("/api/proposals/{id}/complete", proposal.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETING"));

        org.mockito.Mockito.verify(proposalEventPublisher)
                .publishProposalCompleted(
                        org.mockito.Mockito.any(Proposal.class),
                        org.mockito.Mockito.argThat((FeignContractDTO published) ->
                                published.getId().equals(900L)));
    }

    @Test
    void completeAgain_returns400WhenNotAccepted() throws Exception {
        Proposal proposal = saveAcceptedProposal(2000.0);
        stubHappyPathPreChecks(FeignTestFixtures.activeContract(900L, proposal.getId(), 2000.0));

        mockMvc.perform(put("/api/proposals/{id}/complete", proposal.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/proposals/{id}/complete", proposal.getId()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void completeSubmittedProposal_returns400() throws Exception {
        Proposal proposal = saveProposal(ProposalStatus.SUBMITTED, 2000.0);

        mockMvc.perform(put("/api/proposals/{id}/complete", proposal.getId()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void scenarioC_preCheckFailureWithoutActiveContractReturns400AndPublishesNoEvent() throws Exception {
        Proposal proposal = saveAcceptedProposal(2000.0);
        when(jobServiceClient.getJob(JOB_ID)).thenReturn(FeignTestFixtures.openJob(JOB_ID, CLIENT_ID));
        when(userServiceClient.getUser(FREELANCER_ID)).thenReturn(FeignTestFixtures.activeFreelancer(FREELANCER_ID));
        when(contractServiceClient.getActiveContract(proposal.getId()))
                .thenThrow(new FeignException.NotFound(
                        "not found",
                        Request.create(Request.HttpMethod.GET, "/test", java.util.Collections.emptyMap(), null, new RequestTemplate()),
                        null,
                        null));

        mockMvc.perform(put("/api/proposals/{id}/complete", proposal.getId()))
                .andExpect(status().isBadRequest());

        assertThat(proposalRepository.findById(proposal.getId()).orElseThrow().getStatus())
                .isEqualTo(ProposalStatus.ACCEPTED);
        org.mockito.Mockito.verifyNoInteractions(proposalEventPublisher);
    }

    @Test
    void completeWhenJobClosed_returns400AndPublishesNoEvent() throws Exception {
        Proposal proposal = saveAcceptedProposal(2000.0);
        when(jobServiceClient.getJob(JOB_ID)).thenReturn(FeignTestFixtures.jobWithStatus(JOB_ID, CLIENT_ID, "CLOSED"));
        when(userServiceClient.getUser(FREELANCER_ID)).thenReturn(FeignTestFixtures.activeFreelancer(FREELANCER_ID));

        mockMvc.perform(put("/api/proposals/{id}/complete", proposal.getId()))
                .andExpect(status().isBadRequest());

        assertThat(proposalRepository.findById(proposal.getId()).orElseThrow().getStatus())
                .isEqualTo(ProposalStatus.ACCEPTED);
        org.mockito.Mockito.verifyNoInteractions(proposalEventPublisher);
    }

    @Test
    void completeWhenFreelancerDeactivated_returns400AndPublishesNoEvent() throws Exception {
        Proposal proposal = saveAcceptedProposal(2000.0);
        FeignUserDTO deactivated = FeignTestFixtures.activeFreelancer(FREELANCER_ID);
        deactivated.setStatus("DEACTIVATED");
        when(jobServiceClient.getJob(JOB_ID)).thenReturn(FeignTestFixtures.openJob(JOB_ID, CLIENT_ID));
        when(userServiceClient.getUser(FREELANCER_ID)).thenReturn(deactivated);

        mockMvc.perform(put("/api/proposals/{id}/complete", proposal.getId()))
                .andExpect(status().isBadRequest());

        assertThat(proposalRepository.findById(proposal.getId()).orElseThrow().getStatus())
                .isEqualTo(ProposalStatus.ACCEPTED);
        org.mockito.Mockito.verifyNoInteractions(proposalEventPublisher);
    }

    @Test
    void completeWhenCallerIsNotOwner_returns403AndPublishesNoEvent() throws Exception {
        Proposal proposal = saveAcceptedProposal(2000.0);
        stubHappyPathPreChecks(FeignTestFixtures.activeContract(900L, proposal.getId(), 2000.0));

        mockMvc.perform(put("/api/proposals/{id}/complete", proposal.getId())
                        .header("X-User-Id", "99")
                        .header("X-User-Role", "FREELANCER"))
                .andExpect(status().isForbidden());

        assertThat(proposalRepository.findById(proposal.getId()).orElseThrow().getStatus())
                .isEqualTo(ProposalStatus.ACCEPTED);
        org.mockito.Mockito.verifyNoInteractions(proposalEventPublisher);
    }

    @Test
    void completeWhenCallerIsAdmin_returns200() throws Exception {
        Proposal proposal = saveAcceptedProposal(2000.0);
        stubHappyPathPreChecks(FeignTestFixtures.activeContract(900L, proposal.getId(), 2000.0));

        mockMvc.perform(put("/api/proposals/{id}/complete", proposal.getId())
                        .header("X-User-Id", "99")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETING"));
    }

    @Test
    void completeWhenCallerIsOwnerFreelancer_returns200() throws Exception {
        Proposal proposal = saveAcceptedProposal(2000.0);
        stubHappyPathPreChecks(FeignTestFixtures.activeContract(900L, proposal.getId(), 2000.0));

        mockMvc.perform(put("/api/proposals/{id}/complete", proposal.getId())
                        .header("X-User-Id", String.valueOf(FREELANCER_ID))
                        .header("X-User-Role", "FREELANCER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETING"));
    }

    private void stubHappyPathPreChecks(FeignContractDTO contract) {
        when(jobServiceClient.getJob(JOB_ID)).thenReturn(FeignTestFixtures.openJob(JOB_ID, CLIENT_ID));
        when(userServiceClient.getUser(FREELANCER_ID)).thenReturn(FeignTestFixtures.activeFreelancer(FREELANCER_ID));
        when(contractServiceClient.getActiveContract(anyLong())).thenReturn(contract);
    }

    private Proposal saveAcceptedProposal(double bidAmount) {
        return saveProposal(ProposalStatus.ACCEPTED, bidAmount);
    }

    private Proposal saveProposal(ProposalStatus status, double bidAmount) {
        Proposal proposal = new Proposal();
        proposal.setJobId(JOB_ID);
        proposal.setFreelancerId(FREELANCER_ID);
        proposal.setCoverLetter("Ready to complete");
        proposal.setBidAmount(bidAmount);
        proposal.setEstimatedDays(14);
        proposal.setStatus(status);
        if (status == ProposalStatus.ACCEPTED) {
            proposal.setAcceptedAt(LocalDateTime.now());
        }
        return proposalRepository.save(proposal);
    }
}
