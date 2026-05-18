package com.team01.freelance.proposal.service;

import com.team01.freelance.job.model.Job;
import com.team01.freelance.proposal.model.Proposal;
import com.team01.freelance.proposal.model.ProposalStatus;
import com.team01.freelance.contract.repository.ContractRepository;
import com.team01.freelance.proposal.repository.ProposalRepository;
import com.team01.freelance.job.repository.JobRepository;
import com.team01.freelance.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProposalServiceTest {

    @Mock
    private ProposalRepository proposalRepository;
    @Mock
    private JobRepository jobRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ContractRepository contractRepository;

    @InjectMocks
    private ProposalService proposalService;

    @BeforeEach
    void setUp() {
        // MockitoExtension injects mocks into proposalService
    }

    @Test
    void searchProposalsPassesInclusiveDateRangeAsStartAndExclusiveEnd() {
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);
        when(proposalRepository.searchBySubmittedAtRangeAndOptionalStatus(
                eq(LocalDateTime.of(2026, 3, 1, 0, 0)),
                eq(LocalDateTime.of(2026, 4, 1, 0, 0)),
                eq(ProposalStatus.ACCEPTED)))
                .thenReturn(List.of());

        proposalService.searchProposals("ACCEPTED", start, end);

        verify(proposalRepository).searchBySubmittedAtRangeAndOptionalStatus(
                LocalDateTime.of(2026, 3, 1, 0, 0),
                LocalDateTime.of(2026, 4, 1, 0, 0),
                ProposalStatus.ACCEPTED);
    }

    @Test
    void searchProposalsBlankStatusPassesNullStatusToRepository() {
        when(proposalRepository.searchBySubmittedAtRangeAndOptionalStatus(
                eq(LocalDateTime.of(2026, 5, 1, 0, 0)),
                eq(LocalDateTime.of(2026, 5, 11, 0, 0)),
                isNull()))
                .thenReturn(List.of());

        proposalService.searchProposals("   ", LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10));

        verify(proposalRepository).searchBySubmittedAtRangeAndOptionalStatus(
                LocalDateTime.of(2026, 5, 1, 0, 0),
                LocalDateTime.of(2026, 5, 11, 0, 0),
                null);
    }

    @Test
    void searchProposalsNullStatusPassesNullStatusToRepository() {
        when(proposalRepository.searchBySubmittedAtRangeAndOptionalStatus(
                eq(LocalDateTime.of(2026, 1, 1, 0, 0)),
                eq(LocalDateTime.of(2026, 1, 2, 0, 0)),
                isNull()))
                .thenReturn(List.of());

        proposalService.searchProposals(null, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1));

        verify(proposalRepository).searchBySubmittedAtRangeAndOptionalStatus(
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 2, 0, 0),
                null);
    }

    @Test
    void searchProposalsReturnsListFromRepository() {
        Proposal p = new Proposal();
        p.setId(99L);
        when(proposalRepository.searchBySubmittedAtRangeAndOptionalStatus(
                eq(LocalDateTime.of(2026, 2, 1, 0, 0)),
                eq(LocalDateTime.of(2026, 2, 2, 0, 0)),
                isNull()))
                .thenReturn(List.of(p));

        assertThat(proposalService.searchProposals(null, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 1)))
                .containsExactly(p);
    }

    @Test
    void searchProposalsRejectsStartAfterEnd() {
        assertThatThrownBy(() -> proposalService.searchProposals(
                null,
                LocalDate.of(2026, 4, 10),
                LocalDate.of(2026, 4, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("startDate");
    }

    @Test
    void searchProposalsRejectsNullStartDate() {
        assertThatThrownBy(() -> proposalService.searchProposals(null, null, LocalDate.of(2026, 1, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("startDate");
    }

    @Test
    void searchProposalsRejectsNullEndDate() {
        assertThatThrownBy(() -> proposalService.searchProposals(null, LocalDate.of(2026, 1, 1), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("startDate");
    }

    @Test
    void searchProposalsRejectsInvalidStatus() {
        assertThatThrownBy(() -> proposalService.searchProposals(
                "NOT_A_STATUS",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid ProposalStatus");
    }

    @Test
    void acceptProposal_updatesProposalJobAndCreatesContract() {
        Proposal proposal = new Proposal();
        proposal.setId(10L);
        proposal.setJobId(20L);
        proposal.setFreelancerId(30L);
        proposal.setBidAmount(2000.0);
        proposal.setStatus(ProposalStatus.SUBMITTED);

        Job job = new Job();
        job.setId(20L);
        job.setClientId(40L);

        when(proposalRepository.findById(10L)).thenReturn(Optional.of(proposal));
        when(userRepository.findRoleByUserId(30L)).thenReturn("FREELANCER");
        when(jobRepository.findById(20L)).thenReturn(Optional.of(job));
        when(proposalRepository.save(any(Proposal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Proposal result = proposalService.acceptProposal(10L);

        assertThat(result.getStatus()).isEqualTo(ProposalStatus.ACCEPTED);
        assertThat(result.getAcceptedAt()).isNotNull();
        verify(jobRepository).markJobInProgress(20L);
        verify(contractRepository).insertActiveContract(
                eq(20L),
                eq(30L),
                eq(40L),
                eq(10L),
                eq(2000.0),
                any(LocalDateTime.class)
        );
    }

    @Test
    void acceptProposal_rejectsNonAcceptableStatus() {
        Proposal proposal = new Proposal();
        proposal.setId(10L);
        proposal.setStatus(ProposalStatus.ACCEPTED);
        when(proposalRepository.findById(10L)).thenReturn(Optional.of(proposal));

        assertThatThrownBy(() -> proposalService.acceptProposal(10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SUBMITTED or SHORTLISTED");

        verify(userRepository, never()).findRoleByUserId(any());
    }

    @Test
    void acceptProposal_freelancerNotFound() {
        Proposal proposal = new Proposal();
        proposal.setId(10L);
        proposal.setFreelancerId(30L);
        proposal.setStatus(ProposalStatus.SUBMITTED);
        when(proposalRepository.findById(10L)).thenReturn(Optional.of(proposal));
        when(userRepository.findRoleByUserId(30L)).thenReturn(null);

        assertThatThrownBy(() -> proposalService.acceptProposal(10L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Freelancer not found");
    }

    @Test
    void acceptProposal_userNotFreelancer() {
        Proposal proposal = new Proposal();
        proposal.setId(10L);
        proposal.setFreelancerId(30L);
        proposal.setStatus(ProposalStatus.SUBMITTED);
        when(proposalRepository.findById(10L)).thenReturn(Optional.of(proposal));
        when(userRepository.findRoleByUserId(30L)).thenReturn("CLIENT");

        assertThatThrownBy(() -> proposalService.acceptProposal(10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not a freelancer");
    }

    @Test
    void acceptProposal_proposalNotFound() {
        when(proposalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> proposalService.acceptProposal(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Proposal not found");
    }

    @Test
    void searchProposalsAcceptsStatusCaseInsensitive() {
        when(proposalRepository.searchBySubmittedAtRangeAndOptionalStatus(
                eq(LocalDateTime.of(2026, 6, 1, 0, 0)),
                eq(LocalDateTime.of(2026, 6, 16, 0, 0)),
                eq(ProposalStatus.SUBMITTED)))
                .thenReturn(List.of());

        proposalService.searchProposals("submitted", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 15));

        verify(proposalRepository).searchBySubmittedAtRangeAndOptionalStatus(
                LocalDateTime.of(2026, 6, 1, 0, 0),
                LocalDateTime.of(2026, 6, 16, 0, 0),
                ProposalStatus.SUBMITTED);
    }
}
