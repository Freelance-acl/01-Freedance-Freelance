package com.team01.freelance.proposal.service;

import com.team01.freelance.proposal.dto.ProposalDetailsDTO;
import com.team01.freelance.proposal.model.MilestoneStatus;
import com.team01.freelance.proposal.model.Proposal;
import com.team01.freelance.proposal.model.ProposalMilestone;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
}
