package com.team01.freelance.proposal.service;

import com.team01.freelance.proposal.dto.ProposalDetailsDTO;
import com.team01.freelance.proposal.model.MilestoneStatus;
import com.team01.freelance.proposal.dto.ProposalAnalyticsDTO;
import com.team01.freelance.proposal.model.Proposal;
import com.team01.freelance.proposal.model.ProposalMilestone;
import com.team01.freelance.proposal.model.ProposalStatus;
import com.team01.freelance.proposal.model.MilestoneStatus;
import com.team01.freelance.proposal.repository.ProposalAnalyticsProjection;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    void getProposalDetailsBuildsOrderedDtoAndCountsCompletedStatuses() {
        Proposal proposal = new Proposal();
        proposal.setId(10L);
        proposal.setJobId(20L);
        proposal.setFreelancerId(30L);
        proposal.setStatus(ProposalStatus.SUBMITTED);
        proposal.setBidAmount(2000.0);
        proposal.setMetadata(Map.of("source", "unit"));
        proposal.setProposalMilestones(new ArrayList<>(List.of(
                milestone(2L, 3, "Final", MilestoneStatus.PENDING),
                milestone(3L, 1, "Start", MilestoneStatus.COMPLETED),
                milestone(4L, 2, "Review", MilestoneStatus.APPROVED)
        )));
        when(proposalRepository.findByIdWithMilestones(10L)).thenReturn(Optional.of(proposal));

        ProposalDetailsDTO details = proposalService.getProposalDetails(10L);

        assertThat(details.getProposalId()).isEqualTo(10L);
        assertThat(details.getJobId()).isEqualTo(20L);
        assertThat(details.getFreelancerId()).isEqualTo(30L);
        assertThat(details.getStatus()).isEqualTo(ProposalStatus.SUBMITTED);
        assertThat(details.getBidAmount()).isEqualTo(2000.0);
        assertThat(details.getMetadata()).containsEntry("source", "unit");
        assertThat(details.getTotalMilestones()).isEqualTo(3);
        assertThat(details.getCompletedMilestones()).isEqualTo(2);
        assertThat(details.getMilestones())
                .extracting(ProposalDetailsDTO.MilestoneDTO::getMilestoneOrder)
                .containsExactly(1, 2, 3);
    }

    @Test
    void getProposalDetailsReturnsEmptyMilestoneCountsWhenProposalHasNoMilestones() {
        Proposal proposal = new Proposal();
        proposal.setId(10L);
        proposal.setJobId(20L);
        proposal.setFreelancerId(30L);
        proposal.setStatus(ProposalStatus.SUBMITTED);
        proposal.setBidAmount(2000.0);
        proposal.setProposalMilestones(List.of());
        when(proposalRepository.findByIdWithMilestones(10L)).thenReturn(Optional.of(proposal));

        ProposalDetailsDTO details = proposalService.getProposalDetails(10L);

        assertThat(details.getTotalMilestones()).isZero();
        assertThat(details.getCompletedMilestones()).isZero();
        assertThat(details.getMilestones()).isEmpty();
    }

    @Test
    void getProposalDetailsThrowsWhenProposalDoesNotExist() {
        when(proposalRepository.findByIdWithMilestones(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> proposalService.getProposalDetails(404L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Proposal not found");
    }

    private ProposalMilestone milestone(Long id, Integer milestoneOrder, String title, MilestoneStatus status) {
        ProposalMilestone milestone = new ProposalMilestone();
        milestone.setId(id);
        milestone.setMilestoneOrder(milestoneOrder);
        milestone.setTitle(title);
        milestone.setDescription(title + " description");
        milestone.setAmount(100.0);
        milestone.setStatus(status);
        milestone.setMetadata(Map.of("title", title));
        return milestone;
    }
    @Test
    void addMilestonesAssignsOrdersAndPendingStatusFromEmptyProposal() {
        Proposal proposal = proposalWithStatus(20L, 50L, ProposalStatus.SUBMITTED);
        proposal.setBidAmount(2000.0);
        proposal.setProposalMilestones(new ArrayList<>());
        when(proposalRepository.findById(20L)).thenReturn(Optional.of(proposal));
        when(proposalRepository.save(proposal)).thenReturn(proposal);

        Proposal result = proposalService.addMilestones(20L, List.of(
                milestone("Planning", "Plan the work", 800.0),
                milestone("Build", "Build the feature", 700.0)));

        assertThat(result.getProposalMilestones()).hasSize(2);
        assertThat(result.getProposalMilestones()).extracting(ProposalMilestone::getMilestoneOrder)
                .containsExactly(1, 2);
        assertThat(result.getProposalMilestones()).extracting(ProposalMilestone::getStatus)
                .containsExactly(MilestoneStatus.PENDING, MilestoneStatus.PENDING);
        assertThat(result.getProposalMilestones()).allMatch(milestone -> milestone.getProposal() == proposal);
        verify(proposalRepository).save(proposal);
    }

    @Test
    void addMilestonesContinuesFromCurrentMaxOrderAndAllowsTotalEqualToBidAmount() {
        Proposal proposal = proposalWithStatus(21L, 51L, ProposalStatus.SHORTLISTED);
        proposal.setBidAmount(2000.0);
        ProposalMilestone first = milestone("First", "First desc", 800.0);
        first.setMilestoneOrder(1);
        ProposalMilestone second = milestone("Second", "Second desc", 700.0);
        second.setMilestoneOrder(2);
        proposal.setProposalMilestones(new ArrayList<>(List.of(first, second)));
        when(proposalRepository.findById(21L)).thenReturn(Optional.of(proposal));
        when(proposalRepository.save(proposal)).thenReturn(proposal);

        Proposal result = proposalService.addMilestones(21L, List.of(
                milestone("Final", "Final desc", 500.0)));

        assertThat(result.getProposalMilestones()).hasSize(3);
        assertThat(result.getProposalMilestones().get(2).getMilestoneOrder()).isEqualTo(3);
        assertThat(result.getProposalMilestones().stream().mapToDouble(ProposalMilestone::getAmount).sum())
                .isEqualTo(2000.0);
    }

    @Test
    void addMilestonesRejectsTotalAboveBidAmount() {
        Proposal proposal = proposalWithStatus(22L, 52L, ProposalStatus.SUBMITTED);
        proposal.setBidAmount(2000.0);
        ProposalMilestone existing = milestone("Existing", "Existing desc", 2000.0);
        existing.setMilestoneOrder(1);
        proposal.setProposalMilestones(new ArrayList<>(List.of(existing)));
        when(proposalRepository.findById(22L)).thenReturn(Optional.of(proposal));

        assertThatThrownBy(() -> proposalService.addMilestones(22L, List.of(
                milestone("Too much", "Too much desc", 100.0))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bidAmount");

        verify(proposalRepository, never()).save(proposal);
    }

    @Test
    void addMilestonesRejectsAcceptedProposal() {
        Proposal proposal = proposalWithStatus(23L, 53L, ProposalStatus.ACCEPTED);
        proposal.setBidAmount(2000.0);
        when(proposalRepository.findById(23L)).thenReturn(Optional.of(proposal));

        assertThatThrownBy(() -> proposalService.addMilestones(23L, List.of(
                milestone("Title", "Description", 100.0))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SUBMITTED or SHORTLISTED");
    }

    @Test
    void addMilestonesRejectsMissingTitle() {
        Proposal proposal = proposalWithStatus(24L, 54L, ProposalStatus.SUBMITTED);
        proposal.setBidAmount(2000.0);
        proposal.setProposalMilestones(new ArrayList<>());
        when(proposalRepository.findById(24L)).thenReturn(Optional.of(proposal));

        assertThatThrownBy(() -> proposalService.addMilestones(24L, List.of(
                milestone(null, "Description", 100.0))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title");
    }

    @Test
    void addMilestonesThrowsNotFoundWhenProposalMissing() {
        when(proposalRepository.findById(405L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> proposalService.addMilestones(405L, List.of(
                milestone("Title", "Description", 100.0))))
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

    private ProposalMilestone milestone(String title, String description, Double amount) {
        ProposalMilestone milestone = new ProposalMilestone();
        milestone.setTitle(title);
        milestone.setDescription(description);
        milestone.setAmount(amount);
        return milestone;

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
