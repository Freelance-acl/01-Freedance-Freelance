package com.team01.freelance.proposal.saga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.team01.freelance.proposal.messaging.PaymentEventPublisher;
import com.team01.freelance.proposal.model.Proposal;
import com.team01.freelance.proposal.model.ProposalStatus;
import com.team01.freelance.proposal.repository.ProposalRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PayoutAbandonmentReaperTest {

    @Mock
    private ProposalRepository proposalRepository;

    @Mock
    private PaymentEventPublisher paymentEventPublisher;

    @Test
    void reapAbandonedPayouts_publishesSyntheticPaymentFailed() {
        PayoutAbandonmentReaper shortReaper = new PayoutAbandonmentReaper(
                proposalRepository, paymentEventPublisher, Duration.ofSeconds(5));

        Proposal proposal = new Proposal();
        proposal.setId(44L);
        proposal.setContractId(55L);
        proposal.setStatus(ProposalStatus.PAYMENT_PENDING);
        proposal.setPaymentPendingAt(LocalDateTime.now().minusSeconds(10));

        when(proposalRepository.findByStatusAndPaymentPendingAtBefore(
                eq(ProposalStatus.PAYMENT_PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of(proposal));

        shortReaper.reapAbandonedPayouts();

        verify(paymentEventPublisher).publishPaymentFailed(null, 44L, 55L, "payout_abandoned");
    }

    @Test
    void stateMachine_rejectsInvalidTransition() {
        ProposalStateMachine machine = new ProposalStateMachine();
        Proposal proposal = new Proposal();
        proposal.setStatus(ProposalStatus.ACCEPTED);

        assertThatThrownBy(() -> machine.transition(proposal, ProposalStatus.PAID))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void stateMachine_allowsHappyPathTransitions() {
        ProposalStateMachine machine = new ProposalStateMachine();
        Proposal proposal = new Proposal();
        proposal.setStatus(ProposalStatus.ACCEPTED);

        machine.transition(proposal, ProposalStatus.COMPLETING);
        machine.transition(proposal, ProposalStatus.PAYMENT_PENDING);
        machine.transition(proposal, ProposalStatus.PAID);

        assertThat(proposal.getStatus()).isEqualTo(ProposalStatus.PAID);
        assertThat(proposal.getPaymentPendingAt()).isNotNull();
    }
}
