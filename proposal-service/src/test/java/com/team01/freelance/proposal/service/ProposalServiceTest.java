package com.team01.freelance.proposal.service;

import com.team01.freelance.proposal.model.Proposal;
import com.team01.freelance.proposal.model.ProposalStatus;
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

    @Test
    void withdrawProposalSetsSubmittedProposalWithdrawnAndReopensOnlyActiveInProgressJob() {
        Proposal proposal = proposalWithStatus(10L, 25L, ProposalStatus.SUBMITTED);
        when(proposalRepository.findById(10L)).thenReturn(Optional.of(proposal));
        when(proposalRepository.countByJobIdAndStatusIn(eq(25L), eq(List.of(ProposalStatus.SUBMITTED, ProposalStatus.SHORTLISTED))))
                .thenReturn(1L);
        when(proposalRepository.save(proposal)).thenReturn(proposal);

        Proposal result = proposalService.withdrawProposal(10L);

        assertThat(result.getStatus()).isEqualTo(ProposalStatus.WITHDRAWN);
        verify(proposalRepository).save(proposal);
        verify(jobRepository).reopenIfInProgress(25L);
    }

    @Test
    void withdrawProposalAllowsShortlistedProposal() {
        Proposal proposal = proposalWithStatus(11L, 30L, ProposalStatus.SHORTLISTED);
        when(proposalRepository.findById(11L)).thenReturn(Optional.of(proposal));
        when(proposalRepository.countByJobIdAndStatusIn(eq(30L), eq(List.of(ProposalStatus.SUBMITTED, ProposalStatus.SHORTLISTED))))
                .thenReturn(2L);
        when(proposalRepository.save(proposal)).thenReturn(proposal);

        Proposal result = proposalService.withdrawProposal(11L);

        assertThat(result.getStatus()).isEqualTo(ProposalStatus.WITHDRAWN);
        verify(jobRepository, never()).reopenIfInProgress(30L);
    }

    @Test
    void withdrawProposalRejectsAcceptedProposal() {
        Proposal proposal = proposalWithStatus(12L, 40L, ProposalStatus.ACCEPTED);
        when(proposalRepository.findById(12L)).thenReturn(Optional.of(proposal));

        assertThatThrownBy(() -> proposalService.withdrawProposal(12L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SUBMITTED or SHORTLISTED");

        verify(proposalRepository, never()).save(proposal);
        verify(jobRepository, never()).reopenIfInProgress(40L);
    }

    @Test
    void withdrawProposalRejectsRejectedProposal() {
        Proposal proposal = proposalWithStatus(13L, 41L, ProposalStatus.REJECTED);
        when(proposalRepository.findById(13L)).thenReturn(Optional.of(proposal));

        assertThatThrownBy(() -> proposalService.withdrawProposal(13L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SUBMITTED or SHORTLISTED");

        verify(proposalRepository, never()).save(proposal);
        verify(jobRepository, never()).reopenIfInProgress(41L);
    }

    @Test
    void withdrawProposalThrowsNotFoundWhenProposalMissing() {
        when(proposalRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> proposalService.withdrawProposal(404L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Proposal not found");
    }

    private Proposal proposalWithStatus(Long id, Long jobId, ProposalStatus status) {
        Proposal proposal = new Proposal();
        proposal.setId(id);
        proposal.setJobId(jobId);
        proposal.setStatus(status);
        return proposal;
    }
}
