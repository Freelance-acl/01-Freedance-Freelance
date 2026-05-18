package com.team01.freelance.proposal.service;

import com.team01.freelance.proposal.dto.FeeEstimateDTO;
import com.team01.freelance.proposal.model.Proposal;
import com.team01.freelance.proposal.model.ProposalStatus;
import com.team01.freelance.proposal.repository.ProposalRepository;
import com.team01.freelance.job.repository.JobRepository;
import com.team01.freelance.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
    void estimatePlatformFeeWithNoCompetitionUsesTwentyPercentTier() {
        when(proposalRepository.countActiveProposalsInSimilarBidRange(anyDouble(), anyDouble())).thenReturn(0L);

        FeeEstimateDTO estimate = proposalService.estimatePlatformFee(1000.0, 10);

        assertThat(estimate.getBidAmount()).isEqualTo(1000.0);
        assertThat(estimate.getFeePercentage()).isEqualTo(20);
        assertThat(estimate.getPlatformFee()).isEqualTo(200.0);
        assertThat(estimate.getFreelancerPayout()).isEqualTo(800.0);
        assertThat(estimate.getEstimatedDailyRate()).isEqualTo(80.0);
    }

    @Test
    void estimatePlatformFeeWithModerateCompetitionUsesFifteenPercentTier() {
        when(proposalRepository.countActiveProposalsInSimilarBidRange(anyDouble(), anyDouble())).thenReturn(10L);

        FeeEstimateDTO estimate = proposalService.estimatePlatformFee(1000.0, 10);

        assertThat(estimate.getFeePercentage()).isEqualTo(15);
        assertThat(estimate.getPlatformFee()).isEqualTo(150.0);
        assertThat(estimate.getFreelancerPayout()).isEqualTo(850.0);
        assertThat(estimate.getEstimatedDailyRate()).isEqualTo(85.0);
    }

    @Test
    void estimatePlatformFeeRejectsNonPositiveBidAmount() {
        assertThatThrownBy(() -> proposalService.estimatePlatformFee(0.0, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bidAmount");
    }
}
