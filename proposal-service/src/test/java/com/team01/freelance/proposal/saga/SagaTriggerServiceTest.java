package com.team01.freelance.proposal.saga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.team01.freelance.proposal.dto.FeignContractDTO;
import com.team01.freelance.proposal.dto.FeignJobDTO;
import com.team01.freelance.proposal.dto.FeignUserDTO;
import com.team01.freelance.proposal.feign.ContractServiceClient;
import com.team01.freelance.proposal.feign.JobServiceClient;
import com.team01.freelance.proposal.feign.UserServiceClient;
import com.team01.freelance.proposal.messaging.ProposalEventPublisher;
import com.team01.freelance.proposal.model.Proposal;
import com.team01.freelance.proposal.model.ProposalStatus;
import com.team01.freelance.proposal.repository.ProposalRepository;
import com.team01.freelance.proposal.support.FeignTestFixtures;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SagaTriggerServiceTest {

    @Mock
    private ProposalRepository proposalRepository;

    @Spy
    private ProposalStateMachine stateMachine = new ProposalStateMachine();

    @Mock
    private JobServiceClient jobServiceClient;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private ContractServiceClient contractServiceClient;

    @Mock
    private ProposalEventPublisher proposalEventPublisher;

    @InjectMocks
    private SagaTriggerService sagaTriggerService;

    @Test
    void triggerCompletion_setsCompletingAndPublishesEvent() {
        Proposal proposal = acceptedProposal();
        FeignContractDTO contract = FeignTestFixtures.activeContract(20L, 5L, 2000.0);

        when(proposalRepository.findById(5L)).thenReturn(Optional.of(proposal));
        when(jobServiceClient.getJob(7L)).thenReturn(FeignTestFixtures.openJob(7L, 1L));
        when(userServiceClient.getUser(30L)).thenReturn(FeignTestFixtures.activeFreelancer(30L));
        when(contractServiceClient.getActiveContract(5L)).thenReturn(contract);
        when(proposalRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        Proposal result = sagaTriggerService.triggerCompletion(5L);

        assertThat(result.getStatus()).isEqualTo(ProposalStatus.COMPLETING);
        verify(proposalEventPublisher).publishProposalCompleted(
                eq(result),
                argThat((FeignContractDTO dto) -> dto.getId().equals(20L)));
    }

    @Test
    void triggerCompletion_rejectsNonAcceptedStatus() {
        Proposal proposal = acceptedProposal();
        proposal.setStatus(ProposalStatus.SUBMITTED);
        when(proposalRepository.findById(5L)).thenReturn(Optional.of(proposal));

        assertThatThrownBy(() -> sagaTriggerService.triggerCompletion(5L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ACCEPTED");
    }

    @Test
    void triggerCompletion_rejectsClosedJob() {
        Proposal proposal = acceptedProposal();
        FeignJobDTO closedJob = FeignTestFixtures.jobWithStatus(7L, 1L, "CLOSED");

        when(proposalRepository.findById(5L)).thenReturn(Optional.of(proposal));
        when(jobServiceClient.getJob(7L)).thenReturn(closedJob);

        assertThatThrownBy(() -> sagaTriggerService.triggerCompletion(5L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CLOSED");

        verify(proposalEventPublisher, never()).publishProposalCompleted(any(), any());
    }

    @Test
    void triggerCompletion_rejectsDeactivatedFreelancer() {
        Proposal proposal = acceptedProposal();
        FeignUserDTO deactivated = FeignTestFixtures.activeFreelancer(30L);
        deactivated.setStatus("DEACTIVATED");

        when(proposalRepository.findById(5L)).thenReturn(Optional.of(proposal));
        when(jobServiceClient.getJob(7L)).thenReturn(FeignTestFixtures.openJob(7L, 1L));
        when(userServiceClient.getUser(30L)).thenReturn(deactivated);

        assertThatThrownBy(() -> sagaTriggerService.triggerCompletion(5L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ACTIVE");

        verify(proposalEventPublisher, never()).publishProposalCompleted(any(), any());
    }

    @Test
    void triggerCompletion_requiresActiveContract() {
        Proposal proposal = acceptedProposal();

        when(proposalRepository.findById(5L)).thenReturn(Optional.of(proposal));
        when(jobServiceClient.getJob(7L)).thenReturn(FeignTestFixtures.openJob(7L, 1L));
        when(userServiceClient.getUser(30L)).thenReturn(FeignTestFixtures.activeFreelancer(30L));
        when(contractServiceClient.getActiveContract(5L)).thenThrow(notFound());

        assertThatThrownBy(() -> sagaTriggerService.triggerCompletion(5L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ACTIVE contract");

        verify(proposalEventPublisher, never()).publishProposalCompleted(any(), any());
    }

    private static FeignException.NotFound notFound() {
        Request request = Request.create(
                Request.HttpMethod.GET, "/test", Collections.emptyMap(), null, new RequestTemplate());
        return new FeignException.NotFound("not found", request, null, null);
    }

    private static Proposal acceptedProposal() {
        Proposal proposal = new Proposal();
        proposal.setId(5L);
        proposal.setJobId(7L);
        proposal.setFreelancerId(30L);
        proposal.setStatus(ProposalStatus.ACCEPTED);
        return proposal;
    }
}
