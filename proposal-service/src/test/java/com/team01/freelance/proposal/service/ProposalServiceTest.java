package com.team01.freelance.proposal.service;

import com.team01.freelance.proposal.dto.ProposalAnalyticsDTO;
import com.team01.freelance.proposal.model.Proposal;
import com.team01.freelance.proposal.model.ProposalStatus;
import com.team01.freelance.proposal.repository.ProposalAnalyticsProjection;
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
    void getProposalAnalyticsCalculatesMarchScenario() {
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);
        when(proposalRepository.calculateAnalyticsBySubmittedAtRange(
                eq(LocalDateTime.of(2026, 3, 1, 0, 0)),
                eq(LocalDateTime.of(2026, 4, 1, 0, 0))))
                .thenReturn(analyticsProjection(10, 4, 3, 7100.0, 710.0, 40.0));

        ProposalAnalyticsDTO analytics = proposalService.getProposalAnalytics(start, end);

        assertThat(analytics.getTotalProposals()).isEqualTo(10);
        assertThat(analytics.getAcceptedProposals()).isEqualTo(4);
        assertThat(analytics.getRejectedProposals()).isEqualTo(3);
        assertThat(analytics.getTotalBidValue()).isEqualTo(7100.0);
        assertThat(analytics.getAverageBid()).isEqualTo(710.0);
        assertThat(analytics.getAcceptanceRate()).isEqualTo(40.0);
    }

    @Test
    void getProposalAnalyticsReturnsZeroValuesWhenNoProposalsMatch() {
        when(proposalRepository.calculateAnalyticsBySubmittedAtRange(
                eq(LocalDateTime.of(2026, 4, 1, 0, 0)),
                eq(LocalDateTime.of(2026, 5, 1, 0, 0))))
                .thenReturn(analyticsProjection(0, 0, 0, 0.0, 0.0, 0.0));

        ProposalAnalyticsDTO analytics = proposalService.getProposalAnalytics(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30));

        assertThat(analytics.getTotalProposals()).isZero();
        assertThat(analytics.getAcceptedProposals()).isZero();
        assertThat(analytics.getRejectedProposals()).isZero();
        assertThat(analytics.getTotalBidValue()).isZero();
        assertThat(analytics.getAverageBid()).isZero();
        assertThat(analytics.getAcceptanceRate()).isZero();
    }

    @Test
    void getProposalAnalyticsRejectsStartAfterEnd() {
        assertThatThrownBy(() -> proposalService.getProposalAnalytics(
                LocalDate.of(2026, 4, 10),
                LocalDate.of(2026, 4, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("startDate");
    }

    private ProposalAnalyticsProjection analyticsProjection(
            Number totalProposals,
            Number acceptedProposals,
            Number rejectedProposals,
            Number totalBidValue,
            Number averageBid,
            Number acceptanceRate) {
        return new ProposalAnalyticsProjection() {
            @Override
            public Number getTotalProposals() {
                return totalProposals;
            }

            @Override
            public Number getAcceptedProposals() {
                return acceptedProposals;
            }

            @Override
            public Number getRejectedProposals() {
                return rejectedProposals;
            }

            @Override
            public Number getTotalBidValue() {
                return totalBidValue;
            }

            @Override
            public Number getAverageBid() {
                return averageBid;
            }

            @Override
            public Number getAcceptanceRate() {
                return acceptanceRate;
            }
        };
    }
}
