package com.team01.freelance.user.messaging;

import com.team01.freelance.contracts.events.ProposalCancelledEvent;
import com.team01.freelance.contracts.events.ProposalCompletedEvent;
import com.team01.freelance.user.model.FreelancerProposalStat;
import com.team01.freelance.user.repository.FreelancerProposalStatRepository;
import com.team01.freelance.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProposalSagaConsumerTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final FreelancerProposalStatRepository statRepository = mock(FreelancerProposalStatRepository.class);

    private final ProposalSagaConsumer consumer = new ProposalSagaConsumer(userRepository, statRepository);

    @Test
    void handleProposalCompletedUpdatesFreelancerStatsAndStoresCountedProposal() {
        ProposalCompletedEvent event = new ProposalCompletedEvent(
                1L,
                10L,
                5L,
                100L,
                new BigDecimal("2000.00")
        );

        when(statRepository.findById(1L)).thenReturn(Optional.empty());
        when(userRepository.incrementFreelancerStats(eq(5L), eq(new BigDecimal("2000.00"))))
                .thenReturn(1);

        consumer.handleProposalCompleted(event);

        verify(userRepository).incrementFreelancerStats(5L, new BigDecimal("2000.00"));

        ArgumentCaptor<FreelancerProposalStat> statCaptor =
                ArgumentCaptor.forClass(FreelancerProposalStat.class);

        verify(statRepository).save(statCaptor.capture());

        FreelancerProposalStat saved = statCaptor.getValue();

        assertEquals(1L, saved.getProposalId());
        assertEquals(10L, saved.getJobId());
        assertEquals(5L, saved.getFreelancerId());
        assertEquals(100L, saved.getContractId());
        assertEquals(new BigDecimal("2000.00"), saved.getAgreedAmount());
        assertTrue(saved.isCounted());
    }

    @Test
    void handleProposalCompletedIgnoresDuplicateCountedProposal() {
        ProposalCompletedEvent event = new ProposalCompletedEvent(
                1L,
                10L,
                5L,
                100L,
                new BigDecimal("2000.00")
        );

        FreelancerProposalStat existing = new FreelancerProposalStat();
        existing.setProposalId(1L);
        existing.setFreelancerId(5L);
        existing.setAgreedAmount(new BigDecimal("2000.00"));
        existing.setCounted(true);

        when(statRepository.findById(1L)).thenReturn(Optional.of(existing));

        consumer.handleProposalCompleted(event);

        verify(userRepository, never()).incrementFreelancerStats(any(), any());
        verify(statRepository, never()).save(any());
    }

    @Test
    void handleProposalCompletedThrowsWhenFreelancerDoesNotExist() {
        ProposalCompletedEvent event = new ProposalCompletedEvent(
                1L,
                10L,
                5L,
                100L,
                new BigDecimal("2000.00")
        );

        when(statRepository.findById(1L)).thenReturn(Optional.empty());
        when(userRepository.incrementFreelancerStats(eq(5L), eq(new BigDecimal("2000.00"))))
                .thenReturn(0);

        assertThrows(EntityNotFoundException.class, () -> consumer.handleProposalCompleted(event));
    }

    @Test
    void handleProposalCancelledReversesStatsForPreviouslyCountedProposal() {
        ProposalCancelledEvent event = new ProposalCancelledEvent(
                1L,
                10L,
                5L,
                "payment failed"
        );

        FreelancerProposalStat existing = new FreelancerProposalStat();
        existing.setProposalId(1L);
        existing.setJobId(10L);
        existing.setFreelancerId(5L);
        existing.setContractId(100L);
        existing.setAgreedAmount(new BigDecimal("2000.00"));
        existing.setCounted(true);

        when(statRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.decrementFreelancerStats(eq(5L), eq(new BigDecimal("2000.00"))))
                .thenReturn(1);

        consumer.handleProposalCancelled(event);

        verify(userRepository).decrementFreelancerStats(5L, new BigDecimal("2000.00"));

        ArgumentCaptor<FreelancerProposalStat> statCaptor =
                ArgumentCaptor.forClass(FreelancerProposalStat.class);

        verify(statRepository).save(statCaptor.capture());

        FreelancerProposalStat saved = statCaptor.getValue();

        assertEquals(1L, saved.getProposalId());
        assertEquals(5L, saved.getFreelancerId());
        assertEquals("payment failed", saved.getCancellationReason());
        assertFalse(saved.isCounted());
    }

    @Test
    void handleProposalCancelledStoresUncountedCancellationWhenProposalWasNotPreviouslyCompleted() {
        ProposalCancelledEvent event = new ProposalCancelledEvent(
                1L,
                10L,
                5L,
                "client cancelled"
        );

        when(statRepository.findById(1L)).thenReturn(Optional.empty());

        consumer.handleProposalCancelled(event);

        verify(userRepository, never()).decrementFreelancerStats(any(), any());

        ArgumentCaptor<FreelancerProposalStat> statCaptor =
                ArgumentCaptor.forClass(FreelancerProposalStat.class);

        verify(statRepository).save(statCaptor.capture());

        FreelancerProposalStat saved = statCaptor.getValue();

        assertEquals(1L, saved.getProposalId());
        assertEquals(10L, saved.getJobId());
        assertEquals(5L, saved.getFreelancerId());
        assertEquals("client cancelled", saved.getCancellationReason());
        assertFalse(saved.isCounted());
    }

    @Test
    void handleProposalCancelledThrowsWhenFreelancerDoesNotExistDuringReversal() {
        ProposalCancelledEvent event = new ProposalCancelledEvent(
                1L,
                10L,
                5L,
                "payment failed"
        );

        FreelancerProposalStat existing = new FreelancerProposalStat();
        existing.setProposalId(1L);
        existing.setFreelancerId(5L);
        existing.setAgreedAmount(new BigDecimal("2000.00"));
        existing.setCounted(true);

        when(statRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.decrementFreelancerStats(eq(5L), eq(new BigDecimal("2000.00"))))
                .thenReturn(0);

        assertThrows(EntityNotFoundException.class, () -> consumer.handleProposalCancelled(event));
    }

    @Test
    void handleProposalCompletedRejectsMissingProposalId() {
        ProposalCompletedEvent event = new ProposalCompletedEvent(
                null,
                10L,
                5L,
                100L,
                new BigDecimal("2000.00")
        );

        assertThrows(IllegalArgumentException.class, () -> consumer.handleProposalCompleted(event));
    }

    @Test
    void handleProposalCancelledRejectsMissingFreelancerId() {
        ProposalCancelledEvent event = new ProposalCancelledEvent(
                1L,
                10L,
                null,
                "invalid"
        );

        assertThrows(IllegalArgumentException.class, () -> consumer.handleProposalCancelled(event));
    }
}