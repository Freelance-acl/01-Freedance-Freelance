package com.team01.freelance.proposal.saga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.team01.freelance.contract.model.Contract;
import com.team01.freelance.contract.model.ContractStatus;
import com.team01.freelance.contract.repository.ContractRepository;
import com.team01.freelance.job.model.Job;
import com.team01.freelance.job.model.JobStatus;
import com.team01.freelance.job.repository.JobRepository;
import com.team01.freelance.proposal.dto.FeignContractDTO;
import com.team01.freelance.proposal.messaging.ProposalEventPublisher;
import com.team01.freelance.proposal.model.Proposal;
import com.team01.freelance.proposal.model.ProposalStatus;
import com.team01.freelance.proposal.repository.ProposalRepository;
import com.team01.freelance.user.model.User;
import com.team01.freelance.user.model.UserRole;
import com.team01.freelance.user.model.UserStatus;
import com.team01.freelance.user.repository.UserRepository;
import java.util.Optional;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SagaTriggerServiceTest {

    @Mock
    private ProposalRepository proposalRepository;

    @Spy
    private ProposalStateMachine stateMachine = new ProposalStateMachine();

    @Mock
    private JobRepository jobRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private DataSource dataSource;

    @Mock
    private ProposalEventPublisher proposalEventPublisher;

    @InjectMocks
    private SagaTriggerService sagaTriggerService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(sagaTriggerService, "proposalEventPublisher", proposalEventPublisher);
    }

    @Test
    void triggerCompletion_setsCompletingAndPublishesEvent() throws Exception {
        Proposal proposal = acceptedProposal();
        Contract contract = activeContract();

        when(proposalRepository.findById(5L)).thenReturn(Optional.of(proposal));
        when(jobRepository.findById(7L)).thenReturn(Optional.of(openJob()));
        when(userRepository.findById(30L)).thenReturn(Optional.of(activeFreelancer()));
        when(contractRepository.findByProposalId(5L)).thenReturn(Optional.of(contract));
        when(proposalRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        stubNonPostgresDatabase();

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
    void triggerCompletion_rejectsClosedJob() throws Exception {
        Proposal proposal = acceptedProposal();
        Job job = openJob();
        job.setStatus(JobStatus.CLOSED);

        when(proposalRepository.findById(5L)).thenReturn(Optional.of(proposal));
        when(jobRepository.findById(7L)).thenReturn(Optional.of(job));
        stubNonPostgresDatabase();

        assertThatThrownBy(() -> sagaTriggerService.triggerCompletion(5L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CLOSED");

        verify(proposalEventPublisher, never()).publishProposalCompleted(any(), any());
    }

    @Test
    void triggerCompletion_rejectsDeactivatedFreelancer() throws Exception {
        Proposal proposal = acceptedProposal();
        User freelancer = activeFreelancer();
        freelancer.setStatus(UserStatus.DEACTIVATED);

        when(proposalRepository.findById(5L)).thenReturn(Optional.of(proposal));
        when(jobRepository.findById(7L)).thenReturn(Optional.of(openJob()));
        when(userRepository.findById(30L)).thenReturn(Optional.of(freelancer));
        stubNonPostgresDatabase();

        assertThatThrownBy(() -> sagaTriggerService.triggerCompletion(5L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ACTIVE");

        verify(proposalEventPublisher, never()).publishProposalCompleted(any(), any());
    }

    @Test
    void triggerCompletion_requiresActiveContract() throws Exception {
        Proposal proposal = acceptedProposal();

        when(proposalRepository.findById(5L)).thenReturn(Optional.of(proposal));
        when(jobRepository.findById(7L)).thenReturn(Optional.of(openJob()));
        when(userRepository.findById(30L)).thenReturn(Optional.of(activeFreelancer()));
        when(contractRepository.findByProposalId(5L)).thenReturn(Optional.empty());
        stubNonPostgresDatabase();

        assertThatThrownBy(() -> sagaTriggerService.triggerCompletion(5L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ACTIVE contract");

        verify(proposalEventPublisher, never()).publishProposalCompleted(any(), any());
    }

    private void stubNonPostgresDatabase() throws Exception {
        var connection = org.mockito.Mockito.mock(java.sql.Connection.class);
        var metadata = org.mockito.Mockito.mock(java.sql.DatabaseMetaData.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getDatabaseProductName()).thenReturn("H2");
    }

    private static Proposal acceptedProposal() {
        Proposal proposal = new Proposal();
        proposal.setId(5L);
        proposal.setJobId(7L);
        proposal.setFreelancerId(30L);
        proposal.setStatus(ProposalStatus.ACCEPTED);
        return proposal;
    }

    private static Job openJob() {
        Job job = new Job();
        job.setId(7L);
        job.setStatus(JobStatus.IN_PROGRESS);
        return job;
    }

    private static User activeFreelancer() {
        User user = new User();
        user.setId(30L);
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(UserRole.FREELANCER);
        return user;
    }

    private static Contract activeContract() {
        Contract contract = new Contract();
        contract.setId(20L);
        contract.setProposalId(5L);
        contract.setAgreedAmount(2000.0);
        contract.setStatus(ContractStatus.ACTIVE);
        return contract;
    }
}
